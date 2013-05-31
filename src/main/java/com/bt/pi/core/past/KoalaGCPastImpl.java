package com.bt.pi.core.past;

import java.io.IOException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.Continuation.NamedContinuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastException;
import rice.p2p.past.PastPolicy;
import rice.p2p.past.gc.GCId;
import rice.p2p.past.gc.GCPastImpl;
import rice.p2p.past.gc.GCPastMetadata;
import rice.p2p.past.gc.messaging.GCCollectMessage;
import rice.p2p.past.gc.messaging.GCInsertMessage;
import rice.p2p.past.gc.messaging.GCRefreshMessage;
import rice.p2p.past.messaging.FetchHandleMessage;
import rice.p2p.past.messaging.InsertMessage;
import rice.p2p.past.messaging.PastMessage;
import rice.p2p.replication.manager.ReplicationManager;
import rice.pastry.PastryNode;
import rice.pastry.leafset.LeafSet;
import rice.persistence.Cache;
import rice.persistence.StorageManager;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.KoalaContentVersionMismatchException;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.SuperNodeIdFactory;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaContentBase;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.past.content.KoalaMutableContent;
import com.bt.pi.core.past.content.KoalaPiEntityContent;
import com.bt.pi.core.past.continuation.KoalaFreshestHandleLookUpHandlesContinuation;
import com.bt.pi.core.past.continuation.KoalaPiEntityResultContinuation;
import com.bt.pi.core.past.internalcontinuation.KoalaPast;
import com.bt.pi.core.past.internalcontinuation.KoalaPastGetHandlesInsertContinuation;
import com.bt.pi.core.past.internalcontinuation.PiGenericThresholdSensitiveMultiContinuation;
import com.bt.pi.core.past.message.FetchBackupHandleMessage;
import com.bt.pi.core.past.message.InsertBackupMessage;
import com.bt.pi.core.past.message.InsertRequestMessage;
import com.bt.pi.core.pastry_override.ReplicationManagerImpl;
import com.bt.pi.core.scope.NodeScope;

public class KoalaGCPastImpl extends GCPastImpl implements KoalaPast, KoalaDHTStorage {
    private static final String NULL = "null";
    private static final String UNCHECKED = "unchecked";
    private static final double POINT_NINETY_NINE = .99;
    private static final Log LOG = LogFactory.getLog(KoalaGCPastImpl.class);
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private double successfullInsertThreshold;
    private double requiredReadHandlesThreshold;
    private KoalaGCPastImpl localPastInstance;
    private KoalaIdFactory koalaIdFactory;
    private PiBackupHelper backupContentHelper;
    private PastryNode pastryNode;
    private int numberOfBackups = KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS;

    protected class KoalaGCPastDeserializer extends GCPastDeserializer {
        private final Log log = LogFactory.getLog(getClass());

        public KoalaGCPastDeserializer() {
        }

        @Override
        public Message deserialize(InputBuffer buf, short type, int priority, NodeHandle sender) throws IOException {
            log.debug(String.format("deserialize(%s, %d, %d, %s)", buf, type, priority, sender));
            try {
                switch (type) {
                case InsertRequestMessage.TYPE:
                    return InsertRequestMessage.buildKoalaGC(buf, endpoint, contentDeserializer);
                case InsertBackupMessage.TYPE:
                    return InsertBackupMessage.buildKoalaGC(buf, endpoint, contentDeserializer);
                case FetchBackupHandleMessage.TYPE:
                    return FetchBackupHandleMessage.build(buf, endpoint, contentHandleDeserializer);
                default:
                    return super.deserialize(buf, type, priority, sender);
                }
            } catch (IOException e) {
                log.debug(String.format("Exception in deserializer in %s", endpoint.toString()), e);
                throw e;
            }
        }
    }

    // TODO: refactor out insert helper.
    public KoalaGCPastImpl(PastryNode node, StorageManager manager, Cache backup, int replicas, String instance, PastPolicy policy, long collectionInterval, StorageManager trash, int aNumberOfDhtBackups, KoalaIdFactory aKoalaIdFactory,
            KoalaPiEntityFactory aKoalaPiEntityFactory) {
        super(node, manager, backup, replicas, instance, policy, collectionInterval, trash);
        endpoint.setDeserializer(new KoalaGCPastDeserializer());
        pastryNode = node;
        this.numberOfBackups = aNumberOfDhtBackups;
        localPastInstance = this;
        koalaIdFactory = aKoalaIdFactory;
        requiredReadHandlesThreshold = POINT_NINETY_NINE;
        successfullInsertThreshold = POINT_NINETY_NINE;
        this.koalaPiEntityFactory = aKoalaPiEntityFactory;
        backupContentHelper = new PiBackupHelper(this, aNumberOfDhtBackups, this.koalaPiEntityFactory);
    }

    @Override
    protected ReplicationManager buildReplicationManager(Node node, String instance) {
        return new ReplicationManagerImpl(node, this, replicationFactor, instance, new KoalaReplicationPolicy(storage));
    }

    @Override
    public void deliver(Id id, Message message) {
        try {
            processDelivery(id, message);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            throw new RuntimeException(t);
        }
    }

    private void processDelivery(Id id, Message message) {
        if (message instanceof PastMessage) {
            processPastMessage(id, message);
        } else {
            super.deliver(id, message);
        }
    }

    @SuppressWarnings(UNCHECKED)
    private void processPastMessage(Id id, Message message) {
        final PastMessage msg = (PastMessage) message;
        String destination = msg.getDestination() == null ? NULL : msg.getDestination().toStringFull();
        String idToPrint = id == null ? NULL : id.toStringFull();
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("deliver(%s, %s) - Response: %s - Source: %s, Dest: %s", idToPrint, msg, msg.isResponse(), msg.getSource(), destination));

        if (msg.isResponse()) {
            super.deliver(id, message);
        } else if (msg instanceof InsertMessage) {
            processInsertMessage(id, (InsertMessage) msg);
        } else if (msg instanceof FetchHandleMessage) {
            final FetchHandleMessage fmsg = (FetchHandleMessage) msg;
            fetchHandles++;
            backupContentHelper.handleFetchHandleMessage(fmsg);
        } else if (msg instanceof FetchBackupHandleMessage) {
            final FetchBackupHandleMessage fbhm = (FetchBackupHandleMessage) msg;
            backupContentHelper.handleFetchBackupHandleMessage(fbhm);
        } else if (msg instanceof GCRefreshMessage) {
            final GCRefreshMessage rmsg = (GCRefreshMessage) msg;
            final int size = rmsg.getKeys().length;
            Boolean[] result = new Boolean[size];
            Arrays.fill(result, Boolean.TRUE);
            getResponseContinuation(msg).receiveResult(result);
        } else if (msg instanceof GCCollectMessage) {
            LOG.debug(String.format("Ignoring %s message", msg.getClass().getSimpleName()));
        } else {
            super.deliver(id, message);
        }
    }

    private void processInsertMessage(Id id, PastMessage msg) {
        if (msg instanceof InsertBackupMessage) {
            final InsertBackupMessage insertBackupMessage = (InsertBackupMessage) msg;
            storeBackup(insertBackupMessage.getContent(), getResponseContinuation(insertBackupMessage));
        } else if (msg instanceof InsertRequestMessage) {
            final InsertRequestMessage insertRequestMessage = (InsertRequestMessage) msg;
            super.insert(insertRequestMessage.getContent(), insertRequestMessage.getExpiration(), getResponseContinuation(insertRequestMessage));
        } else {
            super.deliver(id, msg);
        }
    }

    /**
     * Internal method which actually performs an insert for a given object. Here so that subclasses can override the
     * types of insert messages which are sent across the wire.
     * 
     * @param obj
     *            The object to insert
     * @param builder
     *            The object which builds the messages
     * @param command
     *            The command to call once done
     * 
     */
    @SuppressWarnings( { UNCHECKED })
    @Override
    protected void doInsert(final Id id, final MessageBuilder builder, final Continuation command, final boolean useSocket) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("doInsert(Id - %s, MessageBuilder - %s, Continuation - %s, useSocket - %s)", id, builder, command, useSocket));
            LOG.debug(String.format("Node: %s attempting to insert locally.", getLocalNodeHandle()));
        }
        // make sure the policy allows the insert
        final GCInsertMessage imsg = (GCInsertMessage) builder.buildMessage();

        if (policy.allowInsert(imsg.getContent())) {
            inserts++;
            final Id msgid = imsg.getContent().getId();
            final KoalaPastGetHandlesInsertContinuation getHandlesContinuation = new KoalaPastGetHandlesInsertContinuation(localPastInstance, command, msgid, builder, useSocket);
            if (LOG.isDebugEnabled())
                LOG.debug("Insert allowed for id: " + msgid);

            /*
             * The implementation of locking & unlocking using lockManager is flawed. Unlock doesn't get invoked on every execution path and even the
             * lock doesn't work well with concurrent access. HOWEVER, both this method (doInsert()) and deliver() in PastImpl should only get invoked
             * by the selector thread, and this becomes a non-issue with single-threaded access. The code with lockManager exists here just because
             * we copied the implementation from PastImpl. 
             */
            lockManager.lock(msgid, new StandardContinuation(command) {
                private final Log log = LogFactory.getLog(getClass());

                public void receiveResult(Object result) {
                    log.debug(String.format("lock - resultReceived(Object - %s)", result));
                    storage.getObject(msgid, new StandardContinuation(parent) {
                        public void receiveResult(Object o) {
                            try {
                                final PastContent content = imsg.getContent().checkInsert(msgid, (PastContent) o);
                                // store locally
                                insertLocallyReplicateAndBackup(msgid, getHandlesContinuation, content, parent);
                            } catch (PastException e) {
                                logPastException(e);
                                lockManager.unlock(msgid);
                                parent.receiveException(e);
                            }
                        }
                    });
                }
            });
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Insert not allowed. Setting result to false.");
            command.receiveException(new PastException("Insert not allowed for id: " + id));
        }
    }

    @SuppressWarnings( { UNCHECKED })
    private void insertLocallyReplicateAndBackup(final Id msgid, final KoalaPastGetHandlesInsertContinuation getHandlesContinuation, final PastContent content, final Continuation parentContinuation) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("insertLocallyReplicateAndBackup(%s, %s)", msgid.toStringFull(), content));
        KoalaGCPastMetadata gcPastMetadata = null;
        if (content instanceof KoalaPiEntityContent) {
            KoalaPiEntityContent koalaPiEntityContent = (KoalaPiEntityContent) content;
            gcPastMetadata = new KoalaGCPastMetadata(koalaPiEntityContent.getVersion(), koalaPiEntityContent.isDeletedAndDeletable(), koalaPiEntityContent.getEntityType());
        }
        if (LOG.isDebugEnabled())
            LOG.debug("metadata : " + gcPastMetadata);
        storage.store(msgid, gcPastMetadata, content, new StandardContinuation(parentContinuation) {
            public void receiveResult(Object result) {
                getHandlesContinuation.setlocalResult(result);
                lockManager.unlock(msgid);
                // after we have stored a local copy successfully replicate
                getHandles(msgid, replicationFactor + 1, getHandlesContinuation);
                // we also backup as well
                backupContentHelper.handleDataInsertion(content);
            }
        });
    }

    private void logPastException(PastException e) {
        if (e instanceof KoalaContentVersionMismatchException)
            LOG.info(e.getMessage(), e);
        else
            LOG.error(e.getMessage(), e);
    }

    /*
     * Note: Backups are different from replicas in that a new id is generated. 
     * This is to allow for a higher garauntee against clustered node failures.
     */
    public void backupContent(int numBackups, NodeScope backupScope, KoalaContentBase content) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("backupContent(numBackups - %d, backupScope - %s, Content.id - %s)", numBackups, backupScope, content.getId()));
        backupContentHelper.backupContent(numBackups, backupScope, content);
    }

    public SortedSet<String> generateBackupIds(int aNumberOfBackups, NodeScope scope, PId pastContentId) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("generateBackupIds(backups- %s, scope - %s, PId - %s )", aNumberOfBackups, scope, pastContentId));
        SortedSet<String> backupIds = null;
        String contentIdStr = pastContentId.asBackupId().getIdAsHex();
        int avzCode = pastContentId.getAvailabilityZone();
        int regionCode = pastContentId.getRegion();
        if (scope.equals(NodeScope.GLOBAL)) {
            backupIds = SuperNodeIdFactory.getSuperNodeCheckPoints(aNumberOfBackups, avzCode, contentIdStr);
        } else if (scope.equals(NodeScope.REGION)) {
            backupIds = SuperNodeIdFactory.getSuperNodeCheckPoints(regionCode, aNumberOfBackups, avzCode, contentIdStr);
        } else {
            backupIds = SuperNodeIdFactory.getSuperNodeCheckPoints(regionCode, avzCode, aNumberOfBackups, avzCode, contentIdStr);
        }

        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Generated backupIds: %s for nodeScope %s and Id %s.", backupIds, scope, pastContentId.getIdAsHex()));
        return backupIds;
    }

    @SuppressWarnings( { UNCHECKED })
    protected void storeBackup(final KoalaContentBase content, Continuation messageContuation) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("storeBackup(%s, %s)", content.getClass().getSimpleName(), content.getId().toStringFull()));

        storage.getObject(content.getId(), new StandardContinuation(messageContuation) {
            @Override
            public void receiveResult(Object result) {
                try {
                    PastContent checkInsert = content.checkInsert(content.getId(), (KoalaContentBase) result);

                    KoalaGCPastMetadata metadata = null;
                    if (content instanceof KoalaPiEntityContent) {
                        KoalaPiEntityContent koalaPiEntityContent = (KoalaPiEntityContent) content;
                        metadata = new KoalaGCPastMetadata(koalaPiEntityContent.getVersion(), koalaPiEntityContent.isDeletedAndDeletable(), koalaPiEntityContent.getEntityType());
                    }

                    storage.store(content.getId(), metadata, checkInsert, new StandardContinuation(this) {
                        @Override
                        public void receiveResult(Object result) {
                            if (LOG.isDebugEnabled())
                                LOG.debug(String.format("Backup of %s  was successful. Result: %s ", content.getId(), result));
                        }
                    });

                } catch (PastException e) {
                    receiveException(e);
                }

                parent.receiveResult(result);
            }
        });
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public void lookupHandles(final Id id, int max, final Continuation command) {
        lookupHandles(id, max, 1, command);
    }

    @SuppressWarnings( { UNCHECKED })
    public void lookupHandles(final Id id, int max, final double percentage, final Continuation command) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retrieving handles of up to " + max + " replicas of the object stored in Past with id " + id);
            LOG.debug("Fetching up to " + max + " handles of " + id.toStringFull() + " percentage: " + percentage);
        }

        getHandles(id, max, new StandardContinuation(command) {
            public void receiveResult(Object o) {
                NodeHandleSet replicas = (NodeHandleSet) o;
                if (LOG.isDebugEnabled())
                    LOG.debug("Receiving replicas " + replicas + " for lookup Id " + id.toStringFull());

                MultiContinuation multi = new PiGenericThresholdSensitiveMultiContinuation(parent, replicas.size(), percentage) {
                    @Override
                    public Object getResult() {
                        PastContentHandle[] p = new PastContentHandle[result.length];

                        for (int i = 0; i < result.length; i++) {
                            if (result[i] instanceof PastContentHandle) {
                                p[i] = (PastContentHandle) result[i];
                            }
                        }
                        if (PiGenericThresholdSensitiveMultiContinuation.LOG.isDebugEnabled())
                            PiGenericThresholdSensitiveMultiContinuation.LOG.debug("Returning PastContentHandles: " + ArrayUtils.toString(p));
                        return p;
                    }
                };

                for (int i = 0; i < replicas.size(); i++)
                    lookupHandle(id, replicas.getHandle(i), multi.getSubContinuation(i));
            }
        });
    }

    @SuppressWarnings(UNCHECKED)
    public void sendPastRequest(NodeHandle handle, PastMessage message, Continuation command) {
        sendRequest(handle, message, command);
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public void insert(PastContent content, Continuation command) {
        insert(content, getVersionFromContent(content), command);
    }

    private long getVersionFromContent(PastContent content) {
        if (content instanceof KoalaMutableContent) {
            KoalaMutableContent koalaMutableContent = (KoalaMutableContent) content;
            return koalaMutableContent.getVersion();
        }
        throw new RuntimeException("expected KoalaMutableContent");
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public void insert(final PastContent content, final long expiration, final Continuation continuation) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("insert(%s, %s, %s)", content, expiration, continuation));
        InsertRequestMessage requestMessage = new InsertRequestMessage(getPastMessageUID(), content, expiration, getLocalNodeHandle(), content.getId());
        sendRequest(content.getId(), requestMessage, continuation);
    }

    @SuppressWarnings(UNCHECKED)
    public void sendPastMessage(Id backupId, PastMessage message, Continuation continuation) {
        Continuation c = new NamedContinuation("PastMessage to " + backupId + " for " + getUID(), continuation);
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Sending request %s to id %s", message, backupId));
        sendRequest(backupId, message, c);
    }

    public int getPastMessageUID() {
        return getUID();
    }

    public void setSuccessfullInsertThreshold(double threshold) {
        successfullInsertThreshold = threshold;
    }

    @Override
    public double getSuccessfulInsertThreshold() {
        return successfullInsertThreshold;
    }

    /**
     * @return the requiredReadHandles
     */
    public double getRequiredReadHandlesThreshold() {
        return requiredReadHandlesThreshold;
    }

    /**
     * @param requiredReadHandlesThreshold
     *            the requiredReadHandles to set
     */
    public void setRequiredReadHandlesPercentage(double threshold) {
        this.requiredReadHandlesThreshold = threshold;
    }

    protected void setKoalaIdFactory(KoalaIdFactory anIdFactory) {
        koalaIdFactory = anIdFactory;
    }

    public KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    public void setExecutor(ScheduledExecutorService anExecutor) {
        backupContentHelper.setExecutor(anExecutor);
    }

    public void setBackupTriggerDelay(int seconds) {
        backupContentHelper.setBackupTriggerDelay(seconds);
    }

    @Override
    public <T extends PiEntity> void get(PId id, Continuation<T, Exception> continuation) {
        KoalaPiEntityResultContinuation<T> piEntityContinuation = new KoalaPiEntityResultContinuation<T>(continuation, koalaPiEntityFactory);
        KoalaFreshestHandleLookUpHandlesContinuation lookUpHandlesContinuation = new KoalaFreshestHandleLookUpHandlesContinuation(this, piEntityContinuation);
        lookupHandles(koalaIdFactory.buildId(id.getIdAsHex()), getReplicationFactor() + 1, requiredReadHandlesThreshold, lookUpHandlesContinuation);
    }

    @Override
    public <T extends PiEntity> void getAny(PId id, Continuation<T, Exception> continuation) {
        lookup(koalaIdFactory.buildId(id.getIdAsHex()), false, new KoalaPiEntityResultContinuation<T>(continuation, koalaPiEntityFactory));
    }

    @Override
    public <T extends PiEntity> void put(PId id, T entity, Continuation<Boolean[], Exception> continuation) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("put for %s (id %s) - version %s", entity.getClass().getSimpleName(), id, entity.getVersion()));

        int backups = 0;
        if (entity.getClass().getAnnotation(Backupable.class) != null) {
            backups = this.numberOfBackups;
        }

        NodeScope backupScope = NodeScope.AVAILABILITY_ZONE;
        if (entity.getClass().getAnnotation(EntityScope.class) != null) {
            backupScope = entity.getClass().getAnnotation(EntityScope.class).scope();
        }

        boolean isDeletedAndDeletable = entity instanceof Deletable && ((Deletable) entity).isDeleted();
        KoalaPiEntityContent content = new KoalaPiEntityContent(koalaIdFactory.buildId(id.getIdAsHex()), koalaPiEntityFactory.getJson(entity), isDeletedAndDeletable, entity.getType(), backups, backupScope, entity.getUrl(), entity.getVersion());
        insert(content, continuation);
    }

    // ---- REPLICATION MANAGER METHODS -----

    /**
     * This upcall is invoked to tell the client to fetch the given id, and to call the given command with the boolean
     * result once the fetch is completed. The client *MUST* call the command at some point in the future, as the
     * manager waits for the command to return before continuing.
     * 
     * @param id
     *            The id to fetch
     */
    @SuppressWarnings( { UNCHECKED })
    @Override
    public void fetch(final Id id, NodeHandle hint, Continuation command) {
        if (!(id instanceof GCId))
            throw new IllegalArgumentException("not GCId");
        if (LOG.isDebugEnabled())
            LOG.debug("Sending out replication fetch request for the id " + id.toStringFull());
        final GCId gcid = (GCId) id;

        if (LOG.isDebugEnabled())
            LOG.debug("expiration/version: " + gcid.getExpiration());

        if (storage.exists(gcid.getId())) {
            if (LOG.isDebugEnabled())
                LOG.debug(gcid + " exists in local storage");
            GCPastMetadata metadata = (GCPastMetadata) storage.getMetadata(gcid.getId());
            if (LOG.isDebugEnabled())
                LOG.debug(metadata);
            if (metadata.getExpiration() < gcid.getExpiration()) {
                doFetch(gcid, hint, command);
            } else {
                command.receiveResult(Boolean.TRUE);
            }
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug(gcid.toStringFull() + " does NOT exist in local storage");
            doFetch(gcid, hint, command);
        }
    }

    @SuppressWarnings( { UNCHECKED })
    private void doFetch(final GCId gcId, NodeHandle hint, Continuation command) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("doFetch(%s, %s, %s)", gcId.toStringFull(), hint, command));
        policy.fetch(gcId.getId(), hint, backup, this, new StandardContinuation(command) {
            public void receiveResult(Object o) {
                if (o == null) {
                    LOG.warn("Could not fetch id " + gcId.getId().toStringFull() + " - policy returned null in namespace " + instance);
                    parent.receiveResult(Boolean.FALSE);
                    return;
                }

                PastContent content = (PastContent) o;
                KoalaGCPastMetadata gcPastMetadata = null;
                if (content instanceof KoalaPiEntityContent) {
                    KoalaPiEntityContent koalaPiEntityContent = (KoalaPiEntityContent) content;

                    if (koalaPiEntityContent.isDeletedAndDeletable()) {
                        LOG.debug(String.format("not storing deleted Deletable replicated id %s", gcId.toStringFull()));
                        return;
                    }

                    if (null == koalaPiEntityContent.getEntityType()) {
                        LOG.warn(String.format("not storing unknown entity type replicated id %s", gcId.toStringFull()));
                        return;
                    }

                    gcPastMetadata = new KoalaGCPastMetadata(koalaPiEntityContent.getVersion(), false, koalaPiEntityContent.getEntityType());
                }
                if (LOG.isDebugEnabled())
                    LOG.debug("inserting replica of id " + gcId.toStringFull());
                storage.getStorage().store(gcId.getId(), gcPastMetadata, content, parent);
            }
        });
    }

    @Override
    @SuppressWarnings(UNCHECKED)
    public Continuation getMessageResponseContinuation(PastMessage message) {
        return getResponseContinuation(message);
    }

    @Override
    public LeafSet getLeafSet() {
        return pastryNode.getLeafSet();
    }

    @Override
    public Id getLocalNodeId() {
        return pastryNode.getId();
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public void readObjectFromStorage(Id id, Continuation continuation) {
        storage.getObject(id, continuation);
    }
}

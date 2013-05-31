package com.bt.pi.core.past;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.messaging.FetchHandleMessage;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.DhtContentHeader;
import com.bt.pi.core.past.content.KoalaContentBase;
import com.bt.pi.core.past.message.FetchBackupHandleMessage;
import com.bt.pi.core.past.message.InsertBackupMessage;
import com.bt.pi.core.scope.NodeScope;

public class PiBackupHelper {
    private static final int ZERO = 0;
    private static final Log LOG = LogFactory.getLog(PiBackupHelper.class);
    private static final String UNCHECKED = "unchecked";
    private static final String RAW_TYPES = "rawtypes";
    private ScheduledExecutorService executor;
    private KoalaDHTStorage dhtStorage;
    private KoalaPiEntityFactory piEntityFactory;
    private int backupTriggerDelayInSeconds;
    private int numberOfBackups = KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS;
    private KoalaIdFactory koalaIdFactory;

    public PiBackupHelper(KoalaDHTStorage koalaDHTStorage, int aNumberOfBackups, KoalaPiEntityFactory koalaPiEntityFactory) {
        dhtStorage = koalaDHTStorage;
        piEntityFactory = null;
        backupTriggerDelayInSeconds = ZERO;
        this.numberOfBackups = aNumberOfBackups;
        this.koalaIdFactory = dhtStorage.getKoalaIdFactory();
        this.piEntityFactory = koalaPiEntityFactory;
    }

    public void setExecutor(ScheduledExecutorService anExecutor) {
        executor = anExecutor;
    }

    public void setBackupTriggerDelay(int seconds) {
        backupTriggerDelayInSeconds = seconds;
    }

    public void handleDataInsertion(PastContent content) {
        if (content instanceof KoalaContentBase)
            checkIfDatatBackupRequired(content);
    }

    private void checkIfDatatBackupRequired(final PastContent content) {
        if (executor != null) {
            executor.schedule(new PiBackupInsertionRunner(content, this), backupTriggerDelayInSeconds, TimeUnit.SECONDS);
        } else
            LOG.warn("Backups are not being processed as the executor is null.");
    }

    public void backupContent(int numBackups, NodeScope backupScope, KoalaContentBase content) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("backupContent(numBackups - %d, backupScope - %s, Content - %s)", numBackups, backupScope, content));
        SortedSet<String> backupIds = dhtStorage.generateBackupIds(numBackups, backupScope, dhtStorage.getKoalaIdFactory().convertToPId(content.getId()));
        for (String backupId : backupIds) {
            // send special message to backedup node.
            Id id = dhtStorage.getKoalaIdFactory().buildIdFromToString(backupId);
            final KoalaContentBase backupContent = content.duplicate();
            backupContent.setId(id);
            backupContent.getContentHeaders().put(DhtContentHeader.ID, id.toStringFull());
            InsertBackupMessage insertBackupMessage = new InsertBackupMessage(dhtStorage.getPastMessageUID(), backupContent, -1, dhtStorage.getLocalNodeHandle(), id);
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("Requesting insert of backup %s at id: %s", backupContent, id.toStringFull()));
            dhtStorage.sendPastMessage(id, insertBackupMessage, new Continuation<Object, Exception>() {
                @Override
                public void receiveException(Exception exception) {
                    if (LOG.isDebugEnabled())
                        LOG.debug(String.format("Failed to send backup %s Exception: %s", backupContent, exception));
                }

                @Override
                public void receiveResult(Object result) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Backup content insert message for: " + result + " sent.");
                }
            });
        }
    }

    @SuppressWarnings( { UNCHECKED, RAW_TYPES })
    public void handleFetchHandleMessage(final FetchHandleMessage fmsg) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("handleFetchHandleMessage(%s)", fmsg));
        dhtStorage.readObjectFromStorage(fmsg.getId(), new StandardContinuation(dhtStorage.getMessageResponseContinuation(fmsg)) {
            public void receiveResult(Object o) {
                PastContent content = (PastContent) o;
                if (content != null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Retrieved data for fetch handles of id " + fmsg.getId());
                    PastContentHandle handle = content.getHandle(dhtStorage);
                    parent.receiveResult(handle);
                } else if (piEntityFactory != null) {
                    PId messageId = koalaIdFactory.convertToPId(fmsg.getId());
                    boolean isBackupable = messageId.isBackupable();
                    if (isBackupable) {
                        relayFetchHandleMessageToBackup(fmsg, parent, messageId);
                    } else {
                        parent.receiveResult(null);
                    }
                } else {
                    LOG.warn(String.format("Unable to properly handle fetchHandleMessage."));
                    parent.receiveResult(null);
                }
            }
        });
    }

    @SuppressWarnings( { UNCHECKED, RAW_TYPES })
    protected void relayFetchHandleMessageToBackup(final FetchHandleMessage fmsg, Continuation parentContinuation, PId messageId) {
        if (LOG.isDebugEnabled())
            LOG.debug("Object not found. Relaying to backup for handle.");
        NodeScope backupScope = messageId.getScope();
        SortedSet<String> backupIds = (SortedSet<String>) dhtStorage.generateBackupIds(numberOfBackups, backupScope, dhtStorage.getKoalaIdFactory().convertToPId(fmsg.getId()));
        // TODO: We should deal with a situation where there are more backups than replicas.
        int position = KoalaIdUtils.getPositionFromId(dhtStorage.getLocalNodeId(), dhtStorage.getLeafSet().asList(), fmsg.getId());

        Id backUpId = koalaIdFactory.buildIdFromToString(new ArrayList<String>(backupIds).get(position % backupIds.size()));
        // create FetchBackupHandle message
        FetchBackupHandleMessage backupMessage = new FetchBackupHandleMessage(dhtStorage.getPastMessageUID(), backUpId, dhtStorage.getLocalNodeHandle(), backUpId);
        // send message to other node to get handle.
        dhtStorage.sendPastMessage(backUpId, backupMessage, new StandardContinuation(parentContinuation) {
            @Override
            public void receiveResult(Object result) {
                if (LOG.isDebugEnabled())
                    LOG.debug("Backup handle being relayed: " + result);
                parent.receiveResult(result);
            }
        });
    }

    @SuppressWarnings( { UNCHECKED, RAW_TYPES })
    public void handleFetchBackupHandleMessage(final FetchBackupHandleMessage fbhm) {
        LOG.info("Backup handle Message :" + fbhm + " id: " + fbhm.getId());
        dhtStorage.readObjectFromStorage(fbhm.getId(), new StandardContinuation(dhtStorage.getMessageResponseContinuation(fbhm)) {
            @Override
            public void receiveResult(Object result) {
                if (result instanceof PastContent) {
                    PastContent content = (PastContent) result;
                    if (LOG.isDebugEnabled())
                        LOG.debug("Retrieved backup data from storage for id: " + fbhm.getId().toStringFull() + " content: " + content);
                    this.parent.receiveResult(content.getHandle(dhtStorage));
                } else {
                    if (LOG.isDebugEnabled())
                        LOG.debug("No backup found returning a null handle");
                    this.parent.receiveResult(null);
                }
            }
        });
    }
}

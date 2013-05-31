//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.node;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mpisws.p2p.transport.liveness.LivenessListener;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;
import org.mpisws.p2p.transport.priority.PriorityTransportLayer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import rice.Continuation;
import rice.environment.params.Parameters;
import rice.pastry.JoinFailedException;
import rice.pastry.NodeHandle;
import rice.pastry.NodeHandleFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.boot.Bootstrapper;
import rice.pastry.dist.DistPastryNodeFactory;
import rice.pastry.socket.SocketNodeHandle;
import rice.pastry.socket.SocketNodeHandleFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.socket.TransportLayerNodeHandle;
import rice.pastry.standard.ProximityNeighborSelector;
import rice.pastry.transport.NodeHandleAdapter;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManager;
import rice.persistence.StorageManagerImpl;
import rice.selector.TimerTask;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.KoalaPastryScribeApplicationBase;
import com.bt.pi.core.bootstrap.NodeBootstrapStrategy;
import com.bt.pi.core.bootstrap.ParameterNodeBootstrapStrategy;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.environment.KoalaEnvironment;
import com.bt.pi.core.exception.KoalaNodeInitializationException;
import com.bt.pi.core.exception.PiConfigurationException;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.node.inet.KoalaNodeInetAddressFactory;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.past.KoalaGCPastImpl;
import com.bt.pi.core.past.KoalaPastPolicy;
import com.bt.pi.core.pastry_override.PersistentStorage;
import com.bt.pi.core.scribe.KoalaScribeImpl;

/*
 * This class is the koala node that sets up applications to
 * run on a particular node.
 */

@Service
public class KoalaNode implements ApplicationContextAware {
    public static final int DEFAULT_NUMBER_OF_DHT_BACKUPS = 4;
    public static final String KOALA_DISK_STORAGE_NAME = "koala-disk-storage";
    public static final int DEFAULT_PORT = 4524;
    private static final String KOALA_PAST_DATA_REPLICAS_COUNT_PARAM = "koala_past_data_replicas_count";
    private static final Log LOG = LogFactory.getLog(KoalaNode.class);

    private static final String KOALA_PAST_DATA_GC_COLLECTION_INTERVAL_PARAM = "koala_past_data_gc_collection_interval";
    private static final String CAN_START_NEW_RING_PARAM = "can_start_new_ring";
    private static final String PASTRY_PORT = "pastry_port";
    private static final String DEFAULT_NEARBY_NODE_TIMEOUT_SECONDS = "60";
    private static final String DEFAULT_METADATA_SYNC_TIME = "" + PersistentStorage.METADATA_SYNC_TIME;
    private static final double POINT_SEVENTY = .70;
    private static final String POINT_SEVENTY_STRING = ".70";
    public static int APPLICATION_SHUTDOWN_TIMEOUT = 20;
    private ApplicationContext applicationContext;

    @Resource
    private KoalaEnvironment environment;
    private PastryNodeFactory factory;
    private PastryNode pastryNode;
    private KoalaGCPastImpl past;

    private KoalaScribeImpl scribe;

    private KoalaPiEntityFactory koalaPiEntityFactory;
    private List<DhtClientFactory> dhtClientFactories;
    private KoalaIdFactory koalaIdFactory;
    // private PiPartitionHandler partitionHandler;

    private Executor executor;

    private int port = DEFAULT_PORT;

    @Resource
    private KoalaNodeInetAddressFactory koalaNodeInetAddressFactory;

    private InetAddress inetAddress;
    private boolean canStartNewRing;
    private boolean startedNewRing;
    private NodeBootstrapStrategy bootstrapStrategy;
    private String preferredBootstraps;
    private int pastDataReplicas = 3;
    private long pastGCCollectionInterval = Long.MAX_VALUE;
    private Map<String, KoalaPastryApplicationBase> localApplicationsClassHash;
    private String nodeIdFile;
    private String pastStorageDir;
    private int findNearbyNodeTimeoutSeconds = Integer.parseInt(DEFAULT_NEARBY_NODE_TIMEOUT_SECONDS);
    private ScheduledExecutorService scheduledExecutorService;
    private long messageQueueCheckPeriodInMinutes;
    private StorageManager storeManager;
    private double successfullInsertThreshold = POINT_SEVENTY;
    private double requiredReadHandlesThreshold = POINT_SEVENTY;
    private int storageMetadataSyncTime = PersistentStorage.METADATA_SYNC_TIME;
    private int numberOfDhtBackups = DEFAULT_NUMBER_OF_DHT_BACKUPS;

    public KoalaNode() {
        applicationContext = null;
        startedNewRing = false;
        localApplicationsClassHash = null;
        bootstrapStrategy = null;
        preferredBootstraps = null;
        scheduledExecutorService = null;
    }

    @Property(key = "number.of.dht.backups", defaultValue = "" + DEFAULT_NUMBER_OF_DHT_BACKUPS)
    public void setNumberOfDhtBackups(int value) {
        this.numberOfDhtBackups = value;
    }

    @Property(key = "find.nearby.node.timeout.seconds", defaultValue = DEFAULT_NEARBY_NODE_TIMEOUT_SECONDS)
    public void setFindNearbyNodeTimeoutSeconds(int aFindNearbyNodeTimeoutSeconds) {
        this.findNearbyNodeTimeoutSeconds = aFindNearbyNodeTimeoutSeconds;
    }

    @Property(key = "storage.metadata.sync.time.millis", defaultValue = DEFAULT_METADATA_SYNC_TIME)
    public void setStorageMetadataSyncTime(int millis) {
        this.storageMetadataSyncTime = millis;
    }

    @Property(key = "successfull.insert.threshold.percentage", defaultValue = POINT_SEVENTY_STRING)
    public void setSuccessfullInsertThreshold(double threshold) {
        successfullInsertThreshold = threshold;
        if (past != null) {
            past.setSuccessfullInsertThreshold(successfullInsertThreshold);
        }
    }

    @Property(key = "required.read.handles.threshold.percentage", defaultValue = POINT_SEVENTY_STRING)
    public void setRequiredReadHandlesThreshold(double threshold) {
        requiredReadHandlesThreshold = threshold;
        if (past != null) {
            past.setRequiredReadHandlesPercentage(threshold);
        }
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Property(key = "messageQueueCheckPeriodInMinutes", defaultValue = "5")
    public void setMessageQueueCheckPeriodInMinutes(long timePeriod) {
        messageQueueCheckPeriodInMinutes = timePeriod;
    }

    @Property(key = "nodeIdFile", defaultValue = "var/run/nodeId.txt")
    public void setNodeIdFile(String aNodeIdFile) {
        nodeIdFile = aNodeIdFile;
    }

    public PastryNode getPastryNode() {
        return pastryNode;
    }

    public void setPastryNode(PastryNode node) {
        this.pastryNode = node;
    }

    public int getPort() {
        return port;
    }

    @Property(key = "node.port", defaultValue = "4524")
    public void setPort(int aPort) {
        this.port = aPort;

    }

    public void setBootstrapStrategy(NodeBootstrapStrategy strategy) {
        this.bootstrapStrategy = strategy;
    }

    public void setPreferredBootstraps(String aPreferredBootstraps) {
        this.preferredBootstraps = aPreferredBootstraps;
    }

    public void setCanStartNewRing(boolean aCanStartNewRing) {
        this.canStartNewRing = aCanStartNewRing;
    }

    public void setpastDataReplicas(int replicas) {
        this.pastDataReplicas = replicas;
    }

    public void setPastGCCollectionInterval(long interval) {
        this.pastGCCollectionInterval = interval;
    }

    public boolean getStartedNewRing() {
        return startedNewRing;
    }

    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    public KoalaGCPastImpl getPast() {
        return past;
    }

    public Collection<NodeHandle> getLeafNodeHandles() {
        Collection<NodeHandle> leafSetNodeHandles = null;
        if (getPastryNode() != null && getPastryNode().getLeafSet() != null) {
            leafSetNodeHandles = getPastryNode().getLeafSet().getUniqueSet();
        }
        return leafSetNodeHandles;
    }

    public NodeHandle getLocalNodeHandle() {
        NodeHandle localNodeHandle = null;
        if (getPastryNode() != null) {
            localNodeHandle = getPastryNode().getLocalHandle();
        }
        return localNodeHandle;
    }

    /*
     * Node Shutdown
     */
    public void stop() {
        LOG.debug("stop()");
        stopApplications();
        pastryNode.destroy();
        environment.getPastryEnvironment().destroy();
        pastryNode = null;
    }

    private void stopApplications() {
        LOG.debug("Stopping all applications");
        final CountDownLatch latch = new CountDownLatch(localApplicationsClassHash.size());

        List<PiApplicationFutureTask> futureTasks = new ArrayList<PiApplicationFutureTask>();
        for (final KoalaPastryApplicationBase application : localApplicationsClassHash.values()) {

            PiApplicationFutureTask future = executeApplicationShuttingDown(latch, application);
            futureTasks.add(future);
        }

        boolean returnedNormally = false;
        try {
            returnedNormally = latch.await(APPLICATION_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }

        if (!returnedNormally) {
            for (PiApplicationFutureTask futureTask : futureTasks) {
                cancelTaskIfNotDone(futureTask);
            }
        }
    }

    private PiApplicationFutureTask executeApplicationShuttingDown(final CountDownLatch latch, final KoalaPastryApplicationBase application) {
        PiApplicationFutureTask future = new PiApplicationFutureTask(new Runnable() {

            @Override
            public void run() {
                try {
                    LOG.debug(String.format("Stopping application:" + application.getApplicationName()));
                    application.applicationContextShuttingDown();
                } catch (Throwable t) {
                    LOG.error("Unable to cleanly shutdown:" + application.getApplicationName(), t);
                } finally {
                    latch.countDown();
                }
            }
        }, application.getApplicationName());

        executor.execute(future);
        return future;
    }

    private void cancelTaskIfNotDone(PiApplicationFutureTask futureTask) {
        if (!futureTask.isDone()) {
            LOG.warn(String.format("Cancelling application %s as it is not finished", futureTask.getApplicationName()));
            if (!futureTask.cancel(true))
                LOG.warn(String.format("Unable to cancel shutting down task for application: %s", futureTask.getApplicationName()));
        }
    }

    /*
     * Node Start up & configuration
     */
    public void start() {
        configureEnvironment();
        String existingId = getExistingId();
        createPastryNode(existingId);
        createStorage();
        startApplications();
        // initializePartitionHandler();

        if (applicationContext != null)
            applicationContext.publishEvent(new NodeStartedEvent(this));
    }

    protected String getExistingId() {
        File file = new File(nodeIdFile);
        if (!file.exists()) {
            LOG.debug("Node id file not found, will generate new id");
            return null;
        }
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            String message = "Error reading node id file, bailing out";
            LOG.error(message, e);
            throw new KoalaNodeInitializationException(message, e);
        }
    }

    protected void saveNodeId(String nodeId) {
        File file = new File(nodeIdFile);
        try {
            FileUtils.writeStringToFile(file, nodeId);
        } catch (IOException e) {
            String message = String.format("Unable to write node id %s to file %s, bailing out", nodeId, nodeIdFile);
            LOG.warn(message, e);
            throw new KoalaNodeInitializationException(message, e);
        }
    }

    public void createPastryNode(String existingId) {
        LOG.debug(String.format("Initializing node factory from existing ID = %s...", existingId));
        LOG.debug(String.format("region: %d", this.koalaIdFactory.getRegion()));
        LOG.debug(String.format("availabilityZone: %d", this.koalaIdFactory.getAvailabilityZoneWithinRegion()));

        inetAddress = koalaNodeInetAddressFactory.lookupInetAddress();
        environment.initPastryEnvironment(inetAddress, getPort());
        LOG.info(String.format("Will bind to address %s, port %d", inetAddress, port));

        environment.getParameters().setString("socket_bindAddress", inetAddress.getHostAddress());
        LOG.debug(String.format("Setting pastry environment variable socket_bindAddress to %s", inetAddress.getHostAddress()));

        // we bootstrap our node
        if (preferredBootstraps != null) {
            environment.getParameters().setString(ParameterNodeBootstrapStrategy.KOALA_PREFERRED_BOOTSTRAPS_PARAM, preferredBootstraps);
            bootstrapStrategy = null;
        }
        if (bootstrapStrategy == null) {
            bootstrapStrategy = new ParameterNodeBootstrapStrategy(environment.getParameters());
        }

        List<InetSocketAddress> bootstrapList = bootstrapStrategy.getBootstrapList();

        if (needToFindRegionAndAvailabilityZone(existingId)) {
            LOG.debug(String.format("Trying to find the Region and Availability Zone from: %s. Existing Region: %d and Availability Zone: %d", bootstrapList, koalaIdFactory.getRegion(), koalaIdFactory.getAvailabilityZoneWithinRegion()));

            findRegionAndAvailabilityZoneFromNearbyNode(bootstrapList);
        } else if (existingId != null) {
            LOG.debug("Setting up Region and Availablity Zone from existing Id:" + existingId);
            koalaIdFactory.setNodeId(existingId);
        }

        try {
            // this really just returns a new instance of SocketPastryNodeFactory.
            factory = DistPastryNodeFactory.getFactory(koalaIdFactory, DistPastryNodeFactory.PROTOCOL_SOCKET, port, environment.getPastryEnvironment());
            pastryNode = factory.newNode();
            LOG.debug("finished creating node");
        } catch (IOException e) {
            LOG.error("Exception :" + e.getMessage() + ": " + e);
            throw new KoalaNodeInitializationException("Failed to initialise node factory and create node", e);
        }

        LOG.info(String.format("Node: %s at %s:%d", pastryNode.getNodeId().toStringFull(), inetAddress, port));
        LOG.info(String.format("Bootstrapping node from %s...", Arrays.toString(bootstrapList.toArray())));

        pastryNode.boot(bootstrapList);

        if (existingId == null) {
            String nodeId = pastryNode.getId().toStringFull();
            koalaIdFactory.setNodeId(nodeId);
            saveNodeId(nodeId);
        }

        synchronized (pastryNode) {
            while (!pastryNode.isReady() && !pastryNode.joinFailed()) {
                // delay so we don't busy-wait
                LOG.debug(String.format("..."));
                try {
                    pastryNode.wait(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }

                // abort if can't join
                if (pastryNode.joinFailed()) {
                    LOG.info(String.format("Bootstrapping failed: %s", pastryNode.joinFailedReason()));
                    if (canStartNewRing) {
                        LOG.info(String.format("Node going to start a new ring"));
                        List<NodeHandle> clearedList = Collections.emptyList();
                        pastryNode.doneNode(clearedList);
                        startedNewRing = true;
                    } else
                        throw new KoalaNodeInitializationException("Failed to join a node ring, and not authorized to create a new one - unable to start");
                }
            }
        }
        setupMessageQueuePrinter();
        LOG.info(String.format("Node %s created", pastryNode));
    }

    /*
        public void initializePartitionHandler() {
            try {
                LOG.debug("Initializing partitionHandler.");
                partitionHandler = new PiPartitionHandler(pastryNode, (SocketPastryNodeFactory) factory, environment.getParameters().getInetSocketAddressArray(ParameterNodeBootstrapStrategy.KOALA_PREFERRED_BOOTSTRAPS_PARAM));
                partitionHandler.start(getPastryNode().getEnvironment().getSelectorManager());
            } catch (UnknownHostException e) {
                LOG.error("Error starting partitionHandler. ", e);
            }
        }

        public void forcePartitionCheck() {
            partitionHandler.run();
        }*/

    @SuppressWarnings("unchecked")
    private void setupMessageQueuePrinter() {
        final PriorityTransportLayer<MultiInetSocketAddress> priority = (PriorityTransportLayer<MultiInetSocketAddress>) getPastryNode().getVars().get(SocketPastryNodeFactory.PRIORITY_TL);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                Collection<MultiInetSocketAddress> nodes = priority.nodesWithPendingMessages();
                LOG.debug(String.format("Number of nodes with pending messages %s", nodes == null ? 0 : nodes.size()));
                for (MultiInetSocketAddress singleNode : nodes) {
                    LOG.debug(String.format("Node: %s has a queue length of %d. With %d bytes pending.", singleNode, priority.queueLength(singleNode), priority.bytesPending(singleNode)));
                }
            }
        }, 1L, messageQueueCheckPeriodInMinutes, TimeUnit.MINUTES);

    }

    private void findRegionAndAvailabilityZoneFromNearbyNode(List<InetSocketAddress> bootstrapList) {
        LOG.debug(String.format("findRegionAndAvailabilityZoneFromNearbyNode(%s)", bootstrapList));
        // override and call SocketPastryNodeFactory$TLBootstrapper.boot(Collection InetSocketAddress)
        // this code 95% copied from Pastry source code apart the final action of setting the Region and Zone and then
        // destroying the temp PastryNode

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            SocketPastryNodeFactory socketPastryNodeFactory = new SocketPastryNodeFactory(koalaIdFactory, port, environment.getPastryEnvironment()) {
                // we need to store this in a mutable final object
                private final List<LivenessListener<NodeHandle>> listener = new ArrayList<LivenessListener<NodeHandle>>();

                @SuppressWarnings("unchecked")
                @Override
                protected Bootstrapper getBootstrapper(PastryNode pn, NodeHandleAdapter tl, NodeHandleFactory handleFactory, ProximityNeighborSelector pns) {
                    LOG.debug(String.format("getBootstrapper(%s, %s, %s, %s)", pn, tl, handleFactory, pns));
                    TLBootstrapper bootstrapper = new TLBootstrapper(pn, tl.getTL(), (SocketNodeHandleFactory) handleFactory, pns) {
                        @Override
                        public void boot(Collection<InetSocketAddress> bootaddresses_temp) {
                            final Collection<InetSocketAddress> bootaddresses;
                            if (bootaddresses_temp == null) {
                                bootaddresses = Collections.EMPTY_LIST;
                            } else {
                                bootaddresses = bootaddresses_temp;
                            }

                            final boolean seed = environment.getParameters().getBoolean("rice_socket_seed") || bootaddresses.isEmpty() || bootaddresses.contains(((SocketNodeHandle) pn.getLocalHandle()).getAddress().getInnermostAddress());

                            if (bootaddresses.isEmpty() || (bootaddresses.size() == 1 && seed)) {
                                LOG.debug("boot() calling pn.doneNode(empty)");
                                bootAsBootstrap();
                                return;
                            }

                            // bogus handles
                            final Collection<SocketNodeHandle> tempBootHandles = new ArrayList<SocketNodeHandle>(bootaddresses.size());

                            // real handles
                            final Collection<rice.pastry.NodeHandle> bootHandles = new HashSet<rice.pastry.NodeHandle>();

                            TransportLayerNodeHandle<MultiInetSocketAddress> local = tl.getLocalIdentifier();
                            InetSocketAddress localAddr = local.getAddress().getInnermostAddress();

                            // fill in tempBootHandles
                            for (InetSocketAddress addr : bootaddresses) {
                                LOG.debug("addr:" + addr + " local:" + localAddr);
                                if (!addr.equals(localAddr)) {
                                    tempBootHandles.add(getTempNodeHandle(addr));
                                }
                            }

                            // this is the end of the task, but we have to declare it here
                            final Continuation<Collection<NodeHandle>, Exception> beginPns = new Continuation<Collection<NodeHandle>, Exception>() {
                                boolean done = false; // to make sure this is only called once

                                /**
                                 * This is usually going to get called twice. The first time when bootHandles is
                                 * complete, the second time on a timeout.
                                 */
                                public void receiveResult(Collection<NodeHandle> initialSet) {
                                    LOG.debug(String.format("receiveResult(%s)", initialSet));
                                    // make sure this only gets called once
                                    if (done)
                                        return;
                                    done = true;
                                    LOG.debug("boot() beginning pns with " + initialSet);

                                    // remove the listener
                                    pn.getLivenessProvider().removeLivenessListener(listener.get(0));

                                    // do proximity neighbor selection
                                    pns.getNearHandles(initialSet, new Continuation<Collection<NodeHandle>, Exception>() {

                                        @SuppressWarnings("deprecation")
                                        public void receiveResult(Collection<NodeHandle> result) {
                                            LOG.debug(String.format("receiveResult(%s)", result));
                                            // done!!!
                                            if (!seed && result.isEmpty()) {
                                                pn.joinFailed(new JoinFailedException("Cannot join ring.  All bootstraps are faulty." + bootaddresses));
                                                return;
                                            }

                                            // here we should have the final result !!!!
                                            NodeHandle[] nodeHandles = result.toArray(new NodeHandle[result.size()]); // preserve
                                            // order
                                            for (NodeHandle nodeHandle : nodeHandles) {
                                                if (nodeHandle.isAlive()) {
                                                    LOG.info(String.format("Found nearby active node: %s", nodeHandle.getNodeId().toStringFull()));
                                                    koalaIdFactory.setRegionFromNodeId(nodeHandle.getId().toStringFull());
                                                    koalaIdFactory.setAvailabilityZoneFromNodeId(nodeHandle.getId().toStringFull());
                                                    latch.countDown();
                                                    break;
                                                }
                                            }
                                        }

                                        public void receiveException(Exception exception) {
                                            LOG.debug(String.format("receiveException(%s)", exception));
                                        }
                                    });
                                }

                                public void receiveException(Exception exception) {
                                    LOG.debug(String.format("receiveException(%s)", exception));
                                }
                            };

                            // Create the listener for the "real" nodes coming online based on WrongAddress messages
                            // from the "Bogus" ones
                            listener.add(new LivenessListener<NodeHandle>() {
                                public void livenessChanged(NodeHandle i2, int val, Map<String, Object> options) {
                                    SocketNodeHandle i = (SocketNodeHandle) i2;
                                    LOG.debug("livenessChanged(" + i + "," + val + ")");
                                    if (val <= LIVENESS_SUSPECTED && i.getEpoch() != -1L) {
                                        boolean complete = false;

                                        // add the new handle
                                        synchronized (bootHandles) {
                                            bootHandles.add((SocketNodeHandle) i);
                                            if (bootHandles.size() == tempBootHandles.size()) {
                                                complete = true;
                                            }
                                        }
                                        if (complete) {
                                            beginPns.receiveResult(bootHandles);
                                        }
                                    }
                                }
                            });

                            LOG.debug("adding liveness listener");
                            // register the listener
                            pn.getLivenessProvider().addLivenessListener(listener.get(0));

                            LOG.debug("checking liveness");
                            // check liveness on the bogus nodes
                            for (SocketNodeHandle h : tempBootHandles) {
                                checkLiveness(h, null);
                            }

                            // need to synchronize, because this can be called on any thread
                            synchronized (bootHandles) {
                                if (bootHandles.size() < tempBootHandles.size()) {
                                    // only wait 10 seconds for the nodes
                                    environment.getSelectorManager().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            LOG.debug("timer expiring, attempting to start pns (it may have already started)");
                                            beginPns.receiveResult(bootHandles);
                                        }
                                    }, 20000);
                                }
                            }

                            // the root node (no boot addresses)
                            if (tempBootHandles.isEmpty()) {
                                LOG.debug("invoking receiveResult (this is probably the first node in the ring)");
                                environment.getSelectorManager().invoke(new Runnable() {
                                    public void run() {
                                        beginPns.receiveResult(bootHandles);
                                    }
                                });
                            }

                            LOG.debug("returning");
                        }
                    };
                    return bootstrapper;
                }
            };

            PastryNode tempNode = socketPastryNodeFactory.newNode();
            tempNode.boot(bootstrapList); // should result in a call to koalaIdFactory.setNodeIdFrom*(..);

            try {
                boolean countedDown = latch.await(findNearbyNodeTimeoutSeconds, TimeUnit.SECONDS);
                if (!countedDown)
                    throw new KoalaNodeInitializationException("Failed to find nearby node to get Region and AvailabilityZone");
            } catch (InterruptedException e) {
                throw new KoalaNodeInitializationException("Failed to initialise node factory and create node", e);
            } finally {
                tempNode.destroy();
                tempNode = null;
                try {
                    Thread.sleep(100); // a little sleep to give the socket time to disconnect!
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOG.debug("leaving findRegionAndAvailabilityZoneFromNearbyNode");
            }
        } catch (IOException e) {
            LOG.error("Exception :" + e.getMessage() + ": " + e);
            throw new KoalaNodeInitializationException("Failed to initialise node factory and create node", e);
        }
    }

    private boolean needToFindRegionAndAvailabilityZone(String existingId) {
        LOG.debug(String.format("needToFindRegionAndAvailabilityZone(%s)", existingId));
        return null == existingId && this.koalaIdFactory.getRegion() == KoalaIdFactory.UNSET && this.koalaIdFactory.getAvailabilityZoneWithinRegion() == KoalaIdFactory.UNSET;
    }

    @Property(key = "past.storage.dir", defaultValue = "./storage")
    public void setPastStorageDir(String value) {
        this.pastStorageDir = value;
    }

    // TODO: Storage needs to be refactored out and springified with parameters
    private void createStorage() {
        // used for generating PastContent object Ids.
        // this implements the "hash function" for our DHT

        // create a different storage root for each node
        // String storageDirectory = configurationProvider.getValue("past.storage.dir", "./storage") +
        // pastryNode.getId().toStringFull();
        String storageDirectory = this.pastStorageDir + pastryNode.getId().toStringFull();

        // create the persistent part
        Storage stor;
        try {
            // we store the files on the disk
            // TODO : remove hardcoded max storage size
            stor = new PersistentDhtStorage(getKoalaIdFactory(), koalaPiEntityFactory, KOALA_DISK_STORAGE_NAME, storageDirectory, -1, true, environment.getPastryEnvironment(), storageMetadataSyncTime);
            storeManager = new StorageManagerImpl(getKoalaIdFactory(), stor, new LRUCache(new MemoryStorage(getKoalaIdFactory()), 4 * 1024, environment.getPastryEnvironment()));

            past = new KoalaGCPastImpl(pastryNode, storeManager, null, pastDataReplicas, "koala-past", new KoalaPastPolicy(), pastGCCollectionInterval, storeManager, numberOfDhtBackups, koalaIdFactory, koalaPiEntityFactory);
            past.setRequiredReadHandlesPercentage(requiredReadHandlesThreshold);
            past.setSuccessfullInsertThreshold(successfullInsertThreshold);
            past.setExecutor(scheduledExecutorService);
        } catch (IOException e) {
            LOG.error(e);
            throw new RuntimeException(e.getMessage(), e);
        }

        if (dhtClientFactories != null)
            for (DhtClientFactory dhtClientFactory : dhtClientFactories)
                dhtClientFactory.setKoalaDhtStorage(past);
    }

    public PersistentDhtStorage getPersistentDhtStorage() {
        if (null == storeManager)
            return null;
        return (PersistentDhtStorage) storeManager.getStorage();
    }

    private void startApplications() {
        scribe = new KoalaScribeImpl(pastryNode, applicationContext);
        scribe.setKoalaIdFactory(koalaIdFactory);

        for (KoalaPastryApplicationBase application : localApplicationsClassHash.values()) {
            if (application instanceof KoalaPastryScribeApplicationBase) {
                ((KoalaPastryScribeApplicationBase) application).setScribe(scribe);
            }
            application.start(getPastryNode(), (KoalaDHTStorage) past, localApplicationsClassHash, getPersistentDhtStorage());
        }
    }

    @Autowired
    public void setPastryApplications(List<KoalaPastryApplicationBase> applications) {
        localApplicationsClassHash = new HashMap<String, KoalaPastryApplicationBase>();
        for (KoalaPastryApplicationBase nodeApp : applications) {
            if (localApplicationsClassHash.containsKey(nodeApp.getApplicationName())) {
                throw new PiConfigurationException(String.format("Application %s has the same application name %s as %s", nodeApp, nodeApp.getApplicationName(), localApplicationsClassHash.get(nodeApp.getApplicationName())));
            }
            localApplicationsClassHash.put(nodeApp.getApplicationName(), nodeApp);
        }
    }

    @Resource
    public void setKoalaPiEntityFactory(KoalaPiEntityFactory piEntityFactory) {
        koalaPiEntityFactory = piEntityFactory;
    }

    @Resource
    public void setDhtWriterFactory(List<DhtClientFactory> aDhtClientFactories) {
        this.dhtClientFactories = aDhtClientFactories;
    }

    @Resource
    public void setEnvironment(KoalaEnvironment env) {
        environment = env;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    public KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    protected void configureEnvironment() {
        Parameters parameters = environment.getParameters();

        if (parameters.contains(KOALA_PAST_DATA_REPLICAS_COUNT_PARAM)) {
            this.setpastDataReplicas(parameters.getInt(KOALA_PAST_DATA_REPLICAS_COUNT_PARAM));
        }

        if (parameters.contains(CAN_START_NEW_RING_PARAM)) {
            this.setCanStartNewRing(parameters.getBoolean(CAN_START_NEW_RING_PARAM));
        }

        if (parameters.contains(KOALA_PAST_DATA_GC_COLLECTION_INTERVAL_PARAM)) {
            this.setPastGCCollectionInterval(parameters.getLong(KOALA_PAST_DATA_GC_COLLECTION_INTERVAL_PARAM));
        }

        if (parameters.contains(PASTRY_PORT)) {
            this.setPort(parameters.getInt(PASTRY_PORT));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext anApplicationContext) throws BeansException {
        applicationContext = anApplicationContext;
    }

    public void setAddressPattern(String anAddressPattern) {
        koalaNodeInetAddressFactory.setAddressPattern(anAddressPattern);
    }

    public KoalaScribeImpl getScribe() {
        return scribe;
    }

    @Resource(name = "taskExecutor")
    public void setExecutor(Executor anExecutor) {
        executor = anExecutor;
    }

}
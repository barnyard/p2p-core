//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.application;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.environment.time.TimeSource;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;
import rice.pastry.PastryNode;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.UnknownApplicationException;
import com.bt.pi.core.continuation.ContinuationUtils;
import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.routing.RoutingMessageRedirector;
import com.bt.pi.core.util.MDCHelper;

/**
 * KoalaPastryApplicationBase is the most basic base-class for creating pastry applications. It has support for
 * application activation and sending messages to other nodes or ids.
 * 
 */
public abstract class KoalaPastryApplicationBase extends ActivationAwareApplicationBase implements Application, MessageContextFactory {
    private static final Log LOG = LogFactory.getLog(KoalaPastryApplicationBase.class);
    private static final String UNKNOWN_DESTINATION_APPLICATION_S = "Unknown destination application: %s";
    private Endpoint endpoint;
    private MessageDeserializer defaultDeserializer;
    private ContinuationRequestWrapperImpl continuationRequestWrapper;
    private KoalaJsonParser koalaJsonParser;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private KoalaDHTStorage storage;
    private KoalaIdFactory koalaIdFactory;
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private Map<String, KoalaPastryApplicationBase> localNodeApplicationsClassMap;
    private PersistentDhtStorage persistentDhtStorage;

    public KoalaPastryApplicationBase() {
        this.endpoint = null;
        this.continuationRequestWrapper = null;
        this.koalaJsonParser = null;
        this.koalaPiEntityFactory = null;
        this.storage = null;
        this.koalaIdFactory = null;
        this.threadPoolTaskExecutor = null;
        this.localNodeApplicationsClassMap = null;
        this.persistentDhtStorage = null;
    }

    public void start(PastryNode aPastryNode, KoalaDHTStorage pastImpl, Map<String, KoalaPastryApplicationBase> nodeApplications, PersistentDhtStorage aPersistentDhtStorage) {
        LOG.debug(String.format("%s - start(%s, %s, %s, %s)", getApplicationName(), aPastryNode, pastImpl, nodeApplications, aPersistentDhtStorage));
        super.start(aPastryNode);
        this.endpoint = aPastryNode.buildEndpoint(this, getApplicationName());
        this.endpoint.setDeserializer(defaultDeserializer);
        this.endpoint.register();
        this.storage = pastImpl;
        this.localNodeApplicationsClassMap = nodeApplications;
        this.persistentDhtStorage = aPersistentDhtStorage;

        onApplicationStarting();

        getApplicationActivator().register(this);
    }

    protected PersistentDhtStorage getPersistentDhtStorage() {
        return persistentDhtStorage;
    }

    public void applicationContextShuttingDown() {
        LOG.info(String.format("Application context shutting down for app %s", getApplicationName()));
        try {
            onApplicationShuttingDown();
        } catch (Throwable t) {
            LOG.error(String.format("Exception on application shutdown for %s", getApplicationName()), t);
        }
    }

    /**
     * Called by the framework AFTER the pastry node has been initialised, but BEFORE application activation has
     * commenced. This method is called synchronously, and the default implementation is a no-op. Override this method
     * to perform application initialisation, initial cleanup and other startup tasks.
     */
    protected void onApplicationStarting() {
    }

    /**
     * Called by the framework upon detection of a shutdown signal. This method is called synchronously, and the default
     * implementation is a no-op. Override this method to perform graceful shutdown tasks.
     */
    protected void onApplicationShuttingDown() {
    }

    /**
     * This is called by pastry when a neighbor node goes down or joins the ring.
     */
    @Override
    public final void update(NodeHandle handle, boolean joined) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.APPLICATION_UPDATE, null);

        try {
            LOG.info(String.format("LocalNode: %s  - update(NodeHandle - %s, joined - %s)", getPastryNode(), handle, joined));
            if (joined) {
                handleNodeArrival(handle.getId().toStringFull());
            } else {
                recordNodeLeavingRing(handle);
            }
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.APPLICATION_UPDATE, null, startTime);
        }
    }

    private void recordNodeLeavingRing(NodeHandle handle) {
        Iterator<rice.pastry.NodeHandle> nearestNodes = getPastryNode().getRouter().getBestRoutingCandidates((rice.pastry.Id) handle.getId());
        LOG.info(String.format("Node %s handling %s leaving.", getNodeIdFull(), handle.getId().toStringFull()));
        boolean nearestNodeFound = false;
        while (nearestNodes.hasNext() && !nearestNodeFound) {
            NodeHandle nh = nearestNodes.next();
            // if I am the nearest node
            if (nh.equals(getNodeHandle())) {
                nearestNodeFound = true;
                LOG.debug(String.format("Node %s is the nearest.", getNodeIdFull()));
                // should we put this in another thread?
                handleNodeDeparture(handle.getId().toStringFull());
            }
            // if the best is live but isn't me...
            else if (nh.checkLiveness()) {
                nearestNodeFound = true;
            }
        }
    }

    /**
     * This method is called when an application must deal with the departure of a neighboring node from the ring. Only
     * one node will receive this invocation per node leaving. The node notified will be the node nearest to the
     * departing node.
     * 
     * @param nodeId
     */
    public abstract void handleNodeDeparture(String nodeId);

    /**
     * This method is called when an application must deal with the arrival of a neighboring node in the ring. All nodes
     * on the ring will be informed.
     * 
     * @param nodeId
     */
    public void handleNodeArrival(String nodeId) {
    };

    public KoalaDHTStorage getStorage() {
        return storage;
    }

    public void setStorage(KoalaDHTStorage aStorage) {
        storage = aStorage;
    }

    public long getTimeStamp() {
        return getPastryNode().getEnvironment().getTimeSource().currentTimeMillis();
    }

    public TimeSource getTimeSource() {
        return getPastryNode().getEnvironment().getTimeSource();
    }

    // TODO: move into ActivationAwareApplicationBase
    public String getNodeIdFull() {
        return getPastryNode().getNodeId().toStringFull();
    }

    @Resource
    public void setDeserializer(MessageDeserializer aMessageDeserializer) {
        LOG.debug(String.format("Set deserialiser %s", aMessageDeserializer));
        defaultDeserializer = aMessageDeserializer;
        if (endpoint != null) {
            endpoint.setDeserializer(aMessageDeserializer);
        }
    }

    @Resource
    public void setContinuationRequestWrapper(ContinuationRequestWrapperImpl aContinuationRequestWrapper) {
        continuationRequestWrapper = aContinuationRequestWrapper;
    }

    public NodeHandle getNodeHandle() {
        return getPastryNode().getLocalNodeHandle();
    }

    /*
     * Deliver message methods
     */
    @Override
    public final void deliver(Id id, Message message) {
        deliver(getKoalaIdFactory().convertToPId(id), message);
    }

    public final void deliver(PId id, Message message) {
        LOG.debug(String.format("NH %s: deliver(%s, %s)", getNodeHandle(), id, message));
        if (message instanceof ApplicationMessage) {
            ApplicationMessage appMessage = (ApplicationMessage) message;
            String transactionUID = appMessage.getTransactionUID();
            MDCHelper.putTransactionUID(transactionUID);

            long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.APPLICATION_DELIVER, transactionUID);

            try {
                KoalaPastryApplicationBase applicationToProcessMessage = appMessage.getDestinationApplicationName() == null ? this : localNodeApplicationsClassMap.get(appMessage.getDestinationApplicationName());
                if (applicationToProcessMessage == null)
                    throw new UnknownApplicationException(String.format(UNKNOWN_DESTINATION_APPLICATION_S, appMessage.getDestinationApplicationName()));

                if (applicationToProcessMessage != this) {
                    LOG.debug(String.format("Delivering message to specified destination application %s instead of %s", applicationToProcessMessage.getApplicationName(), this.getApplicationName()));
                    applicationToProcessMessage.deliver(id, appMessage);
                } else {
                    handleSelfMessage(id, message, appMessage, applicationToProcessMessage);
                }
            } catch (Throwable t) {
                LOG.error("Exception while delivering.", t);
                throw new RuntimeException(t);
            } finally {
                MDCHelper.clearTransactionUID();
                ContinuationUtils.logEndOfContinuation(ContinuationUtils.APPLICATION_DELIVER, transactionUID, startTime);
            }
        } else {
            LOG.debug(String.format("Ignoring non-application message %s", message));
        }
    }

    private void handleSelfMessage(PId id, Message message, ApplicationMessage appMessage, KoalaPastryApplicationBase applicationToProcessMessage) {
        boolean consumed = deliverToWrappers(id, message);
        if (!consumed)
            applicationToProcessMessage.processDeliveryToApplication(id, appMessage);
    }

    protected void processDeliveryToApplication(final PId id, final ApplicationMessage appMessage) {
        ReceivedMessageContext messageContext = new ReceivedMessageContext(this, appMessage);
        deliver(id, messageContext);
    }

    /**
     * Override this method to handle received p2p messages.
     * 
     * @param id
     *            - the id the message was sent to
     * @param receivedMessageContext
     *            - the context of the received message, used to access the sent message and send further messages
     */
    public abstract void deliver(PId id, ReceivedMessageContext receivedMessageContext);

    /**
     * Creates a new MessageContext object that can be used to send messages to other nodes.
     */
    public MessageContext newMessageContext() {
        return newMessageContext(null);
    }

    /**
     * Creates a new MessageContext object with the specified transactionId that can be used to send messages to other
     * nodes.
     */
    public MessageContext newMessageContext(String transactionUID) {
        MessageContext messageContext = new MessageContext(this);
        if (transactionUID != null) {
            LOG.debug(String.format("Generated new message content with existing transaction %s", transactionUID));
            messageContext.setTransactionUID(transactionUID);
        } else {
            LOG.debug(String.format("Generated new message content with transaction %s", messageContext.getTransactionUID()));
        }
        return messageContext;
    }

    protected void putIntoLoggingContext(String key, Object value) {
        if (key != null && value != null)
            MDC.put(key, value);
    }

    @Override
    public final boolean forward(RouteMessage routeMessage) {
        Message innerMessage = null;
        try {
            innerMessage = routeMessage.getMessage(defaultDeserializer);
        } catch (IOException e) {
            LOG.warn("Error trying to extract message from RouteMessage. RouteMessage: " + routeMessage + " Deserializer: " + defaultDeserializer);
        }

        if (innerMessage == null || !(innerMessage instanceof ApplicationMessage))
            return true;

        ApplicationMessage appMessage = (ApplicationMessage) innerMessage;
        String transactionUID = appMessage.getTransactionUID();
        MDCHelper.putTransactionUID(transactionUID);

        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.APPLICATION_FORWARD, transactionUID);
        try {

            PiEntity payload = koalaPiEntityFactory.getPiEntity(appMessage.getJson());
            KoalaPastryApplicationBase applicationToProcessMessage = appMessage.getDestinationApplicationName() == null ? this : localNodeApplicationsClassMap.get(appMessage.getDestinationApplicationName());

            if (applicationToProcessMessage == null) {
                LOG.warn(String.format(UNKNOWN_DESTINATION_APPLICATION_S, appMessage.getDestinationApplicationName()));
                return true;
            }

            if (applicationToProcessMessage != this) {
                LOG.debug(String.format("Forwarding message to specified destination application %s instead of %s", applicationToProcessMessage.getApplicationName(), this.getApplicationName()));
                return applicationToProcessMessage.forward(routeMessage);
            } else {
                // Let's figure out if this message is about to terminate on our node
                if (!(routeMessage instanceof rice.pastry.routing.RouteMessage)) {
                    throw new RuntimeException(String.format("Unexpected RouteMessage implementation: %s", routeMessage.getClass().getName()));
                }

                Id nextHopNodeId = ((rice.pastry.routing.RouteMessage) routeMessage).getNextHop().getNodeId();
                boolean isForThisNode = getNodeId().equals(nextHopNodeId);
                LOG.debug(String.format("Is message for id %s and dest node id %s for local node handle %s? %s", routeMessage.getDestinationId(), nextHopNodeId, getNodeId(), isForThisNode));

                MessageForwardAction messageForwardAction = forwardPiMessage(isForThisNode, routeMessage.getDestinationId(), payload);
                Id newDestinationId = messageForwardAction.getNewDestinationNodeId();
                if (newDestinationId != null) {
                    LOG.debug(String.format("Changing destination id for message %s from %s to %s", appMessage.getUID(), routeMessage.getDestinationId(), newDestinationId));
                    RoutingMessageRedirector routingMessageRedirector = getRoutingMessageRedirector();
                    routingMessageRedirector.reroute(routeMessage, newDestinationId);
                }

                LOG.debug(String.format("Returning %s", messageForwardAction.shouldForwardMessage()));
                return messageForwardAction.shouldForwardMessage();
            }
        } catch (Throwable t) {
            LOG.error("Error while forwarding", t);
            throw new RuntimeException(t.getMessage(), t);
        } finally {
            MDCHelper.clearTransactionUID();
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.APPLICATION_FORWARD, transactionUID, startTime);
        }
    }

    protected RoutingMessageRedirector getRoutingMessageRedirector() {
        return new RoutingMessageRedirector(getPastryNode());
    }

    public MessageForwardAction forwardPiMessage(boolean isForThisNode, Id destinationNodeId, PiEntity payload) {
        return new MessageForwardAction(true);
    }

    public boolean deliverToWrappers(PId id, Message message) {
        boolean consumed = false;
        if (message instanceof KoalaMessage) {
            KoalaMessage koalaMsg = (KoalaMessage) message;
            consumed = continuationRequestWrapper.messageReceived(id, koalaMsg);
        }
        return consumed;
    }

    /**
     * Override this method to specify the activation strategy required for the application. Several implementations are
     * provided 'out of the box', including the always-on one and a strategy that relies on a shared DHT
     * ApplicationRecord that specifies how many active applications are desired within a given ring.
     * 
     * @return an application activator implementation to use
     */
    public abstract ApplicationActivator getApplicationActivator();

    protected KoalaJsonParser getKoalaJsonParser() {
        return koalaJsonParser;
    }

    public KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    public ApplicationStatus getApplicationStatus() {
        return getApplicationActivator().getApplicationStatus(getApplicationName());
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser parser) {
        koalaJsonParser = parser;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory idFactory) {
        koalaIdFactory = idFactory;
    }

    @Resource
    public void setKoalaPiEntityFactory(KoalaPiEntityFactory piEntityFactory) {
        koalaPiEntityFactory = piEntityFactory;
    }

    public KoalaPiEntityFactory getKoalaPiEntityFactory() {
        return koalaPiEntityFactory;
    }

    @Resource
    public void setThreadPoolTaskExecutor(ThreadPoolTaskExecutor athreadPoolTaskExecutor) {
        threadPoolTaskExecutor = athreadPoolTaskExecutor;
    }

    protected Executor getExecutor() {
        return threadPoolTaskExecutor;
    }

    protected Endpoint getEndpoint() {
        return endpoint;
    }

    protected ContinuationRequestWrapperImpl getContinuationRequestWrapper() {
        return continuationRequestWrapper;
    }
}

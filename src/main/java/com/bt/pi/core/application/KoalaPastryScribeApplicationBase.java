package com.bt.pi.core.application;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;

import com.bt.pi.core.continuation.ContinuationUtils;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scribe.KoalaScribeImpl;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.util.MDCHelper;

/**
 * KoalaPastryApplicationBase is a base-class for creating pastry applications that wish to us Scribe (the pub-sub
 * messaging framework). It has support for application activation, sending messages to other nodes or ids, and
 * subscribing and publishing to topics.
 * 
 */
public abstract class KoalaPastryScribeApplicationBase extends KoalaPastryApplicationBase implements KoalaPastryScribeApplication {
    private static final String SUBSCRIBE_FAILED_S = "subscribeFailed(%s)";
    private static final Log LOG = LogFactory.getLog(KoalaPastryScribeApplicationBase.class);

    private KoalaScribeImpl scribe;
    private Collection<Topic> subscribedTopics;

    public KoalaPastryScribeApplicationBase() {
        scribe = null;
        subscribedTopics = new ArrayList<Topic>();
    }

    /**
     * To subscribe to a topic use instead {@link KoalaPastryScribeApplicationBase#subscribe(PId)}
     */
    @Deprecated
    public void subscribe(PiLocation atopic, ScribeMultiClient listener) {
        PId topicPId = getKoalaIdFactory().buildPId(atopic.getUrl()).forLocalScope(atopic.getNodeScope());
        this.subscribe(topicPId, listener);
    }

    /**
     * To unsubscribe to a topic use instead {@link KoalaPastryScribeApplicationBase#unsubscribe(PId)}
     */
    @Deprecated
    public void unsubscribe(PiLocation atopic, ScribeMultiClient listener) {
        PId topicPId = getKoalaIdFactory().buildPId(atopic.getUrl()).forLocalScope(atopic.getNodeScope());
        this.unsubscribe(topicPId, listener);
    }

    /**
     * Subscribes the current KoalaPastryScribeApplication to the topic PId specified.
     * 
     * @param topicId
     *            - PId for the topic to be subscribed to
     */
    public void subscribe(PId topic) {
        subscribe(topic, this);
    }

    /**
     * To subscribe to a topic use instead {@link KoalaPastryScribeApplicationBase#subscribe(PId)} Note:
     * KoalaPastryScribeApplication implements the ScribeMultiClient interface.
     * 
     * @param topicId
     *            - PId for the topic to be subscribed to
     * @param listener
     *            - Class to receive messages from the subscribed topic
     */
    @Deprecated
    public void subscribe(PId topicId, ScribeMultiClient listener) {
        Topic topicToAdd = new Topic(getKoalaIdFactory().buildId(topicId));
        scribe.subscribe(topicToAdd, listener);
        subscribedTopics.add(topicToAdd);
    }

    /**
     * Unsubscribes the current KoalaPastryScribeApplication to the topic PId specified.
     * 
     * @param topicId
     *            - PId for the topic to be unsubscribed from
     */
    public void unsubscribe(PId topicId) {
        unsubscribe(topicId, this);
    }

    /**
     * 
     * Use instead {@link KoalaPastryScribeApplicationBase#subscribe(PId)}
     * 
     * @param topicId
     *            - PId for the topic to be unsubscribe from
     * @param listener
     *            - Listener to be unsubscribed.
     */
    @Deprecated
    public void unsubscribe(PId topicId, ScribeMultiClient listener) {
        Topic topicToRemove = new Topic(getKoalaIdFactory().buildId(topicId));
        scribe.unsubscribe(topicToRemove, listener);
        subscribedTopics.remove(topicToRemove);
    }

    public PiEntity getPiEntityFromJson(String json) {
        return getKoalaPiEntityFactory().getPiEntity(json);
    }

    /**
     * Internal Pi method that converts pastry Scribe messages to PubSubMessageContext messages.
     */
    @Override
    @Deprecated
    public final void deliver(Topic topic, ScribeContent content) {
        LOG.debug(String.format("deliver(Topic - %s, ScribeContent - %s)", topic, content));
        if (content instanceof KoalaScribeContent) {
            KoalaScribeContent koalaScribeContent = (KoalaScribeContent) content;
            String transactionUID = koalaScribeContent.getTransactionUID();
            EntityMethod entityMethod = koalaScribeContent.getEntityMethod();
            MDCHelper.putTransactionUID(transactionUID);

            long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_DELIVER, transactionUID);

            try {
                PiEntity scribeData = getPiEntityFromJson(koalaScribeContent.getJsonData());
                PubSubMessageContext pubSubMessageContext = newPubSubMessageContext(topic, koalaScribeContent.getSourceNodeHandle(), transactionUID);
                deliver(pubSubMessageContext, entityMethod, scribeData);
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
                throw new RuntimeException(t);
            } finally {
                MDCHelper.clearTransactionUID();
                ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_DELIVER, transactionUID, startTime);
            }
        }
    }

    public Collection<Topic> getSubscribedTopics() {
        return subscribedTopics;
    }

    /**
     * Deliver is invoked when this KoalaPastryScribeApplication has been subscribed to a topic and that topic has a
     * message published to it.
     * 
     * @param pubSubMessageContext
     *            - Message context for the received message.
     * @param entityMethod
     *            - Method (i.e. Get, Put) for the PiEntity
     * @param data
     *            - PiEntity sent in the message.
     */
    public abstract void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data);

    /**
     * handleAnycast is invoked when this KoalaPastryScribeApplication has been subscribed to a topic and an anycast
     * message to that topic reaches the current node.
     * 
     * @param pubSubMessageContext
     *            - Message context for the received message.
     * @param entityMethod
     *            - Method (i.e. Get, Put) for the PiEntity
     * @param data
     *            - PiEntity sent in the message.
     * @return
     */
    public abstract boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity);

    @Override
    public final void childAdded(Topic topic, NodeHandle child) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_CHILD_ADDED, null);

        try {
            LOG.debug(String.format("childAdded(%s, %s)", topic, child));
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_CHILD_ADDED, null, startTime);
        }
    }

    @Override
    public final void childRemoved(Topic topic, NodeHandle child) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_CHILD_REMOVED, null);

        try {
            LOG.debug(String.format("childRemoved(%s, %s)", topic, child));
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_CHILD_REMOVED, null, startTime);
        }
    }

    @Override
    public final void subscribeFailed(Collection<Topic> topics) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_FAILED, null);

        try {
            LOG.debug(String.format(SUBSCRIBE_FAILED_S, topics));
            if (topics != null) {
                subscribedTopics.removeAll(topics);
            }
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_FAILED, null, startTime);
        }
    }

    @Override
    public final void subscribeFailed(Topic topic) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_FAILED, null);

        try {
            LOG.debug(String.format(SUBSCRIBE_FAILED_S, topic));
            if (topic != null) {
                subscribedTopics.remove(topic);
            }
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_FAILED, null, startTime);
        }
    }

    @Override
    public final void subscribeSuccess(Collection<Topic> topics) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_SUCCESS, null);

        try {
            LOG.debug(String.format("subscribeSuccess(%s)", topics));
            handleSubscribeSuccess(topics);
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_SUBSCRIBE_SUCCESS, null, startTime);
        }
    }

    public void setScribe(KoalaScribeImpl aScribe) {
        scribe = aScribe;
    }

    public void destroy() {
        LOG.debug("Unsubscribing from topics: " + subscribedTopics);
        scribe.unsubscribe(subscribedTopics, this);
    }

    protected KoalaScribeImpl getScribe() {
        return scribe;
    }

    /**
     * Internal Pi method used to process anycast messages from pastry.
     */
    @Override
    @Deprecated
    public final boolean anycast(Topic topic, ScribeContent content) {
        LOG.debug(String.format("anycastMessage(Topic - %s, ScribeContent - %s)", topic, content));
        if (!(content instanceof KoalaScribeContent))
            throw new IllegalArgumentException("Content is not valid koala content");

        KoalaScribeContent koalaScribeContent = (KoalaScribeContent) content;
        EntityMethod entityMethod = koalaScribeContent.getEntityMethod();
        String transactionUID = koalaScribeContent.getTransactionUID();
        MDCHelper.putTransactionUID(transactionUID);

        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.SCRIBE_ANYCAST, transactionUID);
        try {

            PiEntity scribeData = getPiEntityFromJson(koalaScribeContent.getJsonData());
            PubSubMessageContext pubSubMessageContext = newPubSubMessageContext(topic, koalaScribeContent.getSourceNodeHandle(), transactionUID);
            return handleAnycast(pubSubMessageContext, entityMethod, scribeData);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        } finally {
            MDCHelper.clearTransactionUID();
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.SCRIBE_ANYCAST, transactionUID, startTime);
        }
        return true;
    }

    /**
     * Creates a new PubSubMessageContext that can be used to send messages to a topic.
     * 
     * @param topicPId
     *            - Topic PId that you wish to publish messages to.
     * @param transactionUID
     * @return
     */
    public PubSubMessageContext newPubSubMessageContext(PId topicPId, String transactionUID) {
        PubSubMessageContext res = new PubSubMessageContext(this, topicPId, getNodeHandle(), transactionUID);
        LOG.debug(String.format("Generated new pub sub message content with existing transaction %s", transactionUID));
        return res;
    }

    protected PubSubMessageContext newPubSubMessageContext(PId topicPId, NodeHandle sourceNodeHandle, String transactionUID) {
        LOG.debug(String.format("Generating new pub sub message content for node handle %s with existing transaction %s", sourceNodeHandle, transactionUID));
        return new PubSubMessageContext(this, topicPId, sourceNodeHandle, transactionUID);
    }

    private PubSubMessageContext newPubSubMessageContext(Topic topic, NodeHandle sourceNodeHandle, String transactionUID) {
        LOG.debug(String.format("Generating new pub sub message content for Topic %s and node handle %s with existing transaction %s", topic, sourceNodeHandle, transactionUID));
        return new PubSubMessageContext(this, topic, sourceNodeHandle, transactionUID);
    }

    protected void handleSubscribeSuccess(Collection<Topic> topics) {
    }

}

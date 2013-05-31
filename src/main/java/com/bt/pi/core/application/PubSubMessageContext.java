package com.bt.pi.core.application;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.Topic;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessage;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessageType;

public class PubSubMessageContext extends MessageContext {
    private static final int FIFTY = 50;
    private static final Log LOG = LogFactory.getLog(PubSubMessageContext.class);
    private NodeHandle nodeHandle;
    private Topic topic;

    public PubSubMessageContext(KoalaPastryScribeApplicationBase aHandlingApplication, PiLocation aTopicEntity, String transactionUID) {
        this(aHandlingApplication, aHandlingApplication.getKoalaIdFactory().buildPId(aTopicEntity.getUrl()).forLocalScope(aTopicEntity.getNodeScope()), aHandlingApplication.getNodeHandle(), transactionUID);
    }

    public PubSubMessageContext(KoalaPastryScribeApplicationBase aHandlingApplication, PiLocation aTopicEntity, int globalAvzCode, String transactionUID) {
        this(aHandlingApplication, aHandlingApplication.getKoalaIdFactory().buildPId(aTopicEntity.getUrl()).forScope(aTopicEntity.getNodeScope(), globalAvzCode), aHandlingApplication.getNodeHandle(), transactionUID);
    }

    public PubSubMessageContext(KoalaPastryScribeApplicationBase aHandlingApplication, PId aTopicPId, NodeHandle sourceNodeHandle, String aTransactionUID) {
        this(aHandlingApplication, new Topic(aHandlingApplication.getKoalaIdFactory().buildId(aTopicPId)), aHandlingApplication.getNodeHandle(), aTransactionUID);
    }

    public PubSubMessageContext(KoalaPastryScribeApplicationBase aHandlingApplication, Topic aTopic, NodeHandle sourceNodeHandle, String aTransactionUID) {
        super(aHandlingApplication, aTransactionUID);
        this.topic = aTopic;
        this.nodeHandle = sourceNodeHandle;
    }

    @Override
    public KoalaPastryScribeApplicationBase getHandlingApplication() {
        return (KoalaPastryScribeApplicationBase) super.getHandlingApplication();
    }

    /**
     * Topic Identification for this {@link PubSubMessageContext} should be derived from
     * {@link PubSubMessageContext#getTopicPId()}
     * 
     * @return Topic this context
     */
    @Deprecated
    protected Topic getTopic() {
        return topic;
    }

    public PId getTopicPId() {
        return getHandlingApplication().getKoalaIdFactory().convertToPId(topic.getId());
    }

    public NodeHandle getNodeHandle() {
        return nodeHandle;
    }

    /**
     * Sends an anycast message to the topic.
     * 
     * Note: If this node is subscribed to the topic the message will be delivered here first.
     * 
     * @param entityMethod
     * @param piEntity
     */
    public void sendAnycast(EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("sendAnycast(%s, %s)", entityMethod, piEntity));
        String json = getHandlingApplication().getKoalaPiEntityFactory().getJson(piEntity);
        getHandlingApplication().getScribe().sendAnycast(topic, new KoalaScribeContent(nodeHandle, getTransactionUID(), entityMethod, json));
    }

    /**
     * Sends an anycast message to the topic but originating from a random node. This is to allow a more even
     * distribution of nodes first contacted by the anycast message.
     * 
     * @param entityMethod
     * @param piEntity
     */
    public void randomAnycast(EntityMethod entityMethod, PiEntity piEntity) {
        anycastFromOrigin(getRandomOriginId(), entityMethod, piEntity);
    }

    /**
     * Sends an anycast message to the topic but originating from the specified id. If the node that is closest to the
     * originId is subscribed to the topic it will process the message first.
     * 
     * @param originId
     * @param entityMethod
     * @param piEntity
     */
    public void anycastFromOrigin(PId originId, EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("randomAnycast(%s, %s, %s)", originId, entityMethod, piEntity));
        WrappedScribeContentMessage wrappedScribeContentMessage = new WrappedScribeContentMessage(nodeHandle, topic, WrappedScribeContentMessageType.ANYCAST, entityMethod, getHandlingApplication().getKoalaPiEntityFactory().getJson(piEntity),
                getTransactionUID());
        getHandlingApplication().getScribe().sendScribeMessageFromRandomOrigin(originId, wrappedScribeContentMessage);
    }

    protected PId getRandomOriginId() {
        String topicId = topic.getId().toStringFull();
        int globalAvz = PId.getGlobalAvailabilityZoneCodeFromId(topicId);
        int region = PId.getRegionFromId(topicId);
        int zone = PId.getAvailabilityZoneFromId(topicId);
        String key = RandomStringUtils.randomAlphanumeric(FIFTY);
        PId pidToReturn = getHandlingApplication().getKoalaIdFactory().buildPId(key);
        if (region != -1 && zone != -1) {
            pidToReturn = pidToReturn.forGlobalAvailablityZoneCode(globalAvz);
        } else if (region != -1) {
            pidToReturn = pidToReturn.forRegion(region);
        }
        return pidToReturn;
    }

    /**
     * Publishes the specified PiEntity to the topic this context was created with.
     * 
     * @param entityMethod
     * @param piEntity
     */
    public void publishContent(EntityMethod entityMethod, PiEntity piEntity) {
        LOG.debug(String.format("publishContent(%s, %s)", entityMethod, piEntity));
        String json = getHandlingApplication().getKoalaPiEntityFactory().getJson(piEntity);
        getHandlingApplication().getScribe().publishContent(topic, new KoalaScribeContent(nodeHandle, getTransactionUID(), entityMethod, json));
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PubSubMessageContext))
            return false;
        PubSubMessageContext castOther = (PubSubMessageContext) other;
        return new EqualsBuilder().append(nodeHandle, castOther.nodeHandle).append(topic, castOther.topic).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(nodeHandle).append(topic).toHashCode();
    }

    @Override
    public String toString() {
        return String.format("Node handle: %s, topic: %s", nodeHandle, topic);
    }
}

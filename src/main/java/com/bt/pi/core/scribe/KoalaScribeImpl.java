package com.bt.pi.core.scribe;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.messaging.AnycastFailureMessage;
import rice.p2p.scribe.messaging.AnycastMessage;
import rice.p2p.scribe.messaging.DropMessage;
import rice.p2p.scribe.messaging.PublishMessage;
import rice.p2p.scribe.messaging.PublishRequestMessage;
import rice.p2p.scribe.messaging.ScribeMessage;
import rice.p2p.scribe.messaging.SubscribeAckMessage;
import rice.p2p.scribe.messaging.SubscribeFailedMessage;
import rice.p2p.scribe.messaging.SubscribeMessage;
import rice.p2p.scribe.messaging.UnsubscribeMessage;
import rice.pastry.PastryNode;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessage;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessageType;

public class KoalaScribeImpl extends ScribeImpl {
    private static final Log LOG = LogFactory.getLog(KoalaScribeImpl.class);
    private static final String INSTANCE_STRING_PREFIX = "koala-scribe";
    private KoalaIdFactory koalaIdFactory;
    private ApplicationContext applicationContext;

    public KoalaScribeImpl(PastryNode pastryNode, ApplicationContext aApplicationContext) {
        super(pastryNode, INSTANCE_STRING_PREFIX);
        applicationContext = aApplicationContext;
        setKoalaIdFactory(null);
        setMessageDeserializer();
    }

    private void setMessageDeserializer() {
        endpoint.setDeserializer(new MessageDeserializer() {
            public Message deserialize(InputBuffer buf, short type, int priority, NodeHandle sender) throws IOException {
                Message message = null;
                try {
                    switch (type) {
                    case WrappedScribeContentMessage.TYPE:
                        message = WrappedScribeContentMessage.build(buf, endpoint);
                        break;
                    case AnycastMessage.TYPE:
                        message = AnycastMessage.build(buf, endpoint, getContentDeserializer());
                        break;
                    case SubscribeMessage.TYPE:
                        message = SubscribeMessage.buildSM(buf, endpoint, getContentDeserializer());
                        break;
                    case SubscribeAckMessage.TYPE:
                        message = SubscribeAckMessage.build(buf, endpoint);
                        break;
                    case SubscribeFailedMessage.TYPE:
                        message = SubscribeFailedMessage.build(buf, endpoint);
                        break;
                    case DropMessage.TYPE:
                        message = DropMessage.build(buf, endpoint);
                        break;
                    case PublishMessage.TYPE:
                        message = PublishMessage.build(buf, endpoint, getContentDeserializer());
                        break;
                    case PublishRequestMessage.TYPE:
                        message = PublishRequestMessage.build(buf, endpoint, getContentDeserializer());
                        break;
                    case UnsubscribeMessage.TYPE:
                        message = UnsubscribeMessage.build(buf, endpoint);
                        break;
                    // new in FP 2.1:
                    case AnycastFailureMessage.TYPE:
                        message = AnycastFailureMessage.build(buf, endpoint, getContentDeserializer());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown type:" + type);
                    }
                } catch (IOException e) {
                    LOG.error("Exception in deserializer in " + endpoint.toString() + ":" + INSTANCE_STRING_PREFIX + " " + getContentDeserializer(), e);
                    throw e;
                }

                return message;
            }
        });

    }

    @Resource(type = KoalaIdFactory.class)
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    public Topic createTopic(String topicName) {
        return new Topic(koalaIdFactory.buildId(koalaIdFactory.buildPId(topicName).forLocalScope(NodeScope.REGION)));
    }

    public Topic subscribeToTopic(String topicName, ScribeMultiClient scribeClient) {
        LOG.debug(String.format("subscribeToTopic(%s, %s)", topicName, scribeClient));

        Topic topic = createTopic(topicName);
        subscribe(topic, scribeClient);
        return topic;
    }

    public void publishContent(Topic topic, ScribeContent content) {
        if (topic != null) {
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("publishContent(%s, %s)", topic, content));
            publish(topic, content);
        } else {
            throw new IllegalArgumentException("Cannot publish to null topic.");
        }
    }

    public void sendAnycast(Topic topic, ScribeContent content) {
        if (topic != null) {
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("sendAnycast(%s, %s)", topic, content));
            anycast(topic, content);
        } else {
            throw new IllegalArgumentException("Cannot anycast to null topic.");
        }
    }

    public void sendScribeMessageFromRandomOrigin(PId id, WrappedScribeContentMessage content) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("sendScribeMessageFromRandomOrigin(%s, %s)", id, content));
        endpoint.route(koalaIdFactory.buildId(id), content, null);
    }

    @Override
    public void deliver(Id id, Message message) {
        if (message instanceof AnycastMessage) {
            AnycastMessage aMessage = (AnycastMessage) message;
            if (aMessage instanceof SubscribeMessage)
                super.deliver(id, message);
            else {
                LOG.warn(String.format("Anycast failed at root for topic %s, message %s", aMessage.getTopic(), aMessage.getContent()));
                handleAnycastFailure(aMessage);
            }
        } else if (message instanceof WrappedScribeContentMessage) {
            WrappedScribeContentMessage wrappedScribeContentMessage = (WrappedScribeContentMessage) message;
            ScribeContent scribeContent = new KoalaScribeContent(wrappedScribeContentMessage.getSource(), UUID.randomUUID().toString(), wrappedScribeContentMessage.getEntityMethod(), wrappedScribeContentMessage.getJsonData());

            if (wrappedScribeContentMessage.getWrappedScribeContentMessageType().equals(WrappedScribeContentMessageType.ANYCAST)) {
                anycast(wrappedScribeContentMessage.getTopic(), scribeContent);
            } else if (wrappedScribeContentMessage.getWrappedScribeContentMessageType().equals(WrappedScribeContentMessageType.PUBLISH))
                publish(wrappedScribeContentMessage.getTopic(), scribeContent);
        } else {
            super.deliver(id, message);
        }
    }

    private void handleAnycastFailure(AnycastMessage anycastMessage) {
        // We send a anycast failure message
        // TODO: make this faster if using raw serialized message, use fast ctor
        AnycastFailureMessage aFailMsg = new AnycastFailureMessage(endpoint.getLocalNodeHandle(), anycastMessage.getTopic(), anycastMessage.getContent());
        endpoint.route(null, aFailMsg, anycastMessage.getInitialRequestor());

        Topic topic = anycastMessage.getTopic();
        if (topic == null)
            return;

        if (applicationContext != null)
            applicationContext.publishEvent(new AnycastFailureEvent(this, topic.getId(), anycastMessage.getContent()));
        else
            LOG.warn("Application context is null, not publishing anycast failure message event");
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean forward(RouteMessage message) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("forward(%s)", message));

        Message internalMessage;
        try {
            internalMessage = message.getMessage(endpoint.getDeserializer());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        if (LOG.isDebugEnabled())
            LOG.debug("Forward called with " + internalMessage + " " + internalMessage.getClass().getName());
        if (internalMessage instanceof ScribeMessage) {
            policy.intermediateNode((ScribeMessage) internalMessage);
        }

        if (internalMessage instanceof AnycastMessage) {
            AnycastMessage aMessage = (AnycastMessage) internalMessage;

            // get the topic manager associated with this topic
            if (aMessage.getTopic() == null) {
                throw new RuntimeException("topic is null!");
            }
            TopicManager manager = (TopicManager) topicManagers.get(aMessage.getTopic());

            // if it's a subscribe message, we must handle it differently
            if (internalMessage instanceof SubscribeMessage) {
                SubscribeMessage sMessage = (SubscribeMessage) internalMessage;
                return handleForwardSubscribeMessage(sMessage);
            } else {
                // Note that since forward() is called also on the outgoing path, it
                // could be that the last visited node of the anycast message is itself,
                // then in that case we return true
                if (LOG.isDebugEnabled())
                    LOG.debug("DEBUG: Anycast message.forward(1)");
                // There is a special case in the modified exhaustive anycast traversal
                // algorithm where the traversal ends at the node which happens to be
                // the best choice in the bag of prospectiveresponders. In this scenario
                // the local node's anycast() method will be visited again

                if (endpoint.getLocalNodeHandle().equals(aMessage.getLastVisited()) && !endpoint.getLocalNodeHandle().equals(aMessage.getInitialRequestor())) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Bypassing forward logic of anycast message becuase local node is the last visited node " + aMessage.getLastVisited() + " of in the anycast message ");
                    if (isRoot(aMessage.getTopic())) {
                        if (LOG.isDebugEnabled())
                            LOG.debug("Local node is the root of anycast group " + aMessage.getTopic());
                    }
                    return true;
                }

                // if we are not associated with this topic at all, let the
                // anycast continue
                if (manager == null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Manager of anycast group is null");
                    return true;
                }

                Collection<ScribeMultiClient> clients = manager.getClients();

                // see if one of our clients will accept the anycast
                for (ScribeMultiClient client : clients) {
                    if (client.anycast(aMessage.getTopic(), aMessage.getContent())) {
                        if (LOG.isDebugEnabled())
                            LOG.debug("Accepting anycast message from " + aMessage.getSource() + " for topic " + aMessage.getTopic());
                        return false;
                    }
                }

                // if we are the orginator for this anycast and it already has a destination,
                // we let it go ahead
                if (aMessage.getSource().getId().equals(endpoint.getId()) && (message.getNextHopHandle() != null) && (!localHandle.equals(message.getNextHopHandle()))) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("DEBUG: Anycast message.forward(2), before returning true");
                    return true;
                }

                if (LOG.isDebugEnabled())
                    LOG.debug("Rejecting anycast message from " + aMessage.getSource() + " for topic " + aMessage.getTopic());
            }

            // add the local node to the visited list
            aMessage.addVisited(endpoint.getLocalNodeHandle());

            // allow the policy to select the order in which the nodes are visited
            policy.directAnycast(aMessage, manager.getParent(), manager.getChildren());

            // reset the source of the message to be us
            aMessage.setSource(endpoint.getLocalNodeHandle());

            // get the next hop
            NodeHandle handle = aMessage.getNext();

            // make sure that the next node is alive
            while ((handle != null) && (!handle.isAlive())) {
                handle = aMessage.getNext();
            }

            if (LOG.isDebugEnabled())
                LOG.debug("Forwarding anycast message for topic " + aMessage.getTopic() + "on to " + handle);

            if (handle == null) {
                LOG.warn(String.format("Anycast failed at intermediate node for topic %s, message %s", aMessage.getTopic(), aMessage.getContent()));
                handleAnycastFailure(aMessage);
            } else {
                // What is going on here?
                if (LOG.isDebugEnabled())
                    LOG.debug("forward() routing " + aMessage + " to " + handle);
                endpoint.route(null, aMessage, handle);
            }

            return false;
        }

        return true;
    }
}

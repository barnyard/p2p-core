package com.bt.pi.core.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.p2p.commonapi.MessageReceipt;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.message.KoalaMessageBase;
import com.bt.pi.core.messaging.KoalaMessageContinuation;
import com.bt.pi.core.messaging.KoalaMessageSender;

public class MessageContext extends MessageContextBase implements KoalaMessageSender {
    private static final Log LOG = LogFactory.getLog(MessageContext.class);

    public MessageContext(KoalaPastryApplicationBase aHandlingApp) {
        this(aHandlingApp, null);
    }

    public MessageContext(KoalaPastryApplicationBase aHandlingApp, String aTransactionUID) {
        super(aHandlingApp, aTransactionUID);
    }

    protected <T extends PiEntity> KoalaMessageContinuation<T> getKoalaMessageContinuation(Continuation<T, Exception> appContinuation) {
        return new KoalaMessageContinuation<T>(appContinuation, getHandlingApplication().getKoalaPiEntityFactory());
    }

    protected void enrichMessage(KoalaMessage msg) {
        if (msg instanceof KoalaMessageBase) {
            LOG.debug(String.format("Setting transaction uid to %s and injecting parser", getTransactionUID()));
            ((KoalaMessageBase) msg).setTransactionUID(getTransactionUID());
            ((KoalaMessageBase) msg).setKoalaJsonParser(getHandlingApplication().getKoalaJsonParser());
        } else
            LOG.debug("Ignoring non KoalaMessageBase");
    }

    /**
     * Sends the specified entity to the same application used to create this context on the node nearest the specified
     * Id.
     * 
     * @param id
     * @param method
     * @param payload
     */
    public <T extends PiEntity> void routePiMessage(PId id, EntityMethod method, T payload) {
        routePiMessageToApplication(id, method, payload, (String) null);
    }

    /**
     * Sends the specified entity to the same application used to create this context on the node nearest the specified
     * Id.
     * 
     * @param id
     * @param method
     * @param payload
     * @param appContinuation
     *            - Continuation that will be invoked when a response message is sent back to this node as a result of
     *            the entity sent.
     */
    public <T extends PiEntity> void routePiMessage(PId id, EntityMethod method, T payload, Continuation<T, Exception> appContinuation) {
        routePiMessageToApplication(id, method, payload, null, appContinuation);
    }

    public <T extends PiEntity> void routePiMessageToApplication(PId id, EntityMethod method, T payload, String destAppName) {
        routeMessage(id, createApplicationMessage(method, null, payload, destAppName, getHandlingApplication().getApplicationName()), null);
    }

    /**
     * Sends the specified entity to the application specified on the node nearest the specified Id.
     * 
     * @param id
     * @param method
     * @param payload
     * @param destAppName
     *            - Name of the application to deliver the message to.
     * @param appContinuation
     *            - Continuation that will be invoked when a response message is sent back to this node as a result of
     *            the entity sent.
     */
    public <T extends PiEntity> void routePiMessageToApplication(PId id, EntityMethod method, T payload, String destAppName, Continuation<T, Exception> appContinuation) {
        routeMessage(id, createApplicationMessage(method, null, payload, destAppName, getHandlingApplication().getApplicationName()), getKoalaMessageContinuation(appContinuation));
    }

    /**
     * To send a message use {@link MessageContext#routePiMessage(PId, EntityMethod, PiEntity)}
     */
    @Override
    @Deprecated
    public void routeMessage(PId id, KoalaMessage msg) {
        LOG.debug(String.format("routeMsg(Id - %s,KoalaMessage - %s)", id, msg));
        enrichMessage(msg);

        MessageReceipt receipt = getHandlingApplication().getEndpoint().route(getHandlingApplication().getKoalaIdFactory().buildId(id), msg, null);
        LOG.debug("routeMsg result: " + receipt);
    }

    /**
     * To send a message with a continuation use
     * {@link MessageContext#routePiMessage(PId, EntityMethod, PiEntity, Continuation)}
     */
    @Deprecated
    public void routeMessage(PId id, KoalaMessage msg, Continuation<KoalaMessage, Exception> appContinuation) {
        LOG.debug(String.format("routeMsg(Id - %s,KoalaMessage - %s, continuation - %s)", id, msg, appContinuation));
        if (appContinuation == null) {
            LOG.debug(String.format("Continuation was null, not routing through continuation wrapper"));
            routeMessage(id, msg);
            return;
        }

        enrichMessage(msg);
        getHandlingApplication().getContinuationRequestWrapper().sendRequest(id, msg, this, appContinuation);
    }

    /**
     * Sends the specified entity to the application used to create this context on the node as denoted by the
     * nodeHandle.
     * 
     * @param id
     * @param method
     * @param payload
     */
    public MessageReceipt routeMsgDirect(NodeHandle nh, EntityMethod method, PiEntity payload) {
        return routeMsgDirect(nh, method, payload, null);
    }

    /**
     * Sends the specified entity to the application specified on the node as denoted by the nodeHandle.
     * 
     * @param NodeHandle
     * @param method
     * @param payload
     * @param destAppName
     *            - Name of the application to deliver the message to.
     */
    public MessageReceipt routeMsgDirect(NodeHandle nh, EntityMethod method, PiEntity payload, String destAppName) {
        return routeMsgDirect(nh, createApplicationMessage(method, null, payload, destAppName, getHandlingApplication().getApplicationName()));
    }

    /**
     * To send a message to a particular NodeHandle use
     * {@link MessageContext#routeMsgDirect(NodeHandle, EntityMethod, PiEntity)}
     */
    @Deprecated
    public MessageReceipt routeMsgDirect(NodeHandle nh, KoalaMessage msg) {
        LOG.debug(String.format("routMsgDirect(NodeHandle - %s,KoalaMessage - %s)", nh, msg));
        enrichMessage(msg);
        MessageReceipt receipt = getHandlingApplication().getEndpoint().route(null, msg, nh);
        LOG.debug("routeMsgDirect result: " + receipt);
        return receipt;
    }

    protected ApplicationMessage createApplicationMessage(EntityMethod method, EntityResponseCode entityResponseCode, PiEntity payload, String destAppName, String sourceAppName) {
        String sourceIdString = getHandlingApplication().getNodeId().toStringFull();
        String jsonPayload = getHandlingApplication().getKoalaJsonParser().getJson(payload);
        LOG.debug(String.format("Creating new app message with params %s, %s, %s, %s, %s, %s", jsonPayload, sourceIdString, method, entityResponseCode, destAppName, sourceAppName));
        return new ApplicationMessage(jsonPayload, sourceIdString, method, entityResponseCode, destAppName, sourceAppName);
    }
}

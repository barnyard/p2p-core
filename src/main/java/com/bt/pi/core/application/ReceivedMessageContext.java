package com.bt.pi.core.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.ApplicationMessage;

public class ReceivedMessageContext extends MessageContext {
    private static final Log LOG = LogFactory.getLog(ReceivedMessageContext.class);
    private ApplicationMessage applicationMessage;
    private PiEntity receivedEntity;

    public ReceivedMessageContext(KoalaPastryApplicationBase aHandlingApp, ApplicationMessage anApplicationMessage) {
        super(aHandlingApp, anApplicationMessage.getTransactionUID());
        applicationMessage = anApplicationMessage;

        receivedEntity = getHandlingApplication().getKoalaPiEntityFactory().getPiEntity(applicationMessage.getJson());
    }

    public PiEntity getReceivedEntity() {
        return receivedEntity;
    }

    public EntityMethod getMethod() {
        return applicationMessage.getMethod();
    }

    public EntityResponseCode getResponseCode() {
        return applicationMessage.getResponseCode();
    }

    public ApplicationMessage getApplicationMessage() {
        return applicationMessage;
    }

    public void sendResponse(EntityResponseCode entityResponseCode, PiEntity entity) {
        LOG.debug(String.format("sendResponse(%s, %s)", entityResponseCode, entity));
        if (entityResponseCode == null)
            throw new IllegalArgumentException("Entity response code cannot be null");

        String responseDestApplicationName = applicationMessage.getSourceApplicationName();

        ApplicationMessage responseApplicationMessage = createApplicationMessage(null, entityResponseCode, entity, responseDestApplicationName, getHandlingApplication().getApplicationName());
        responseApplicationMessage.setCorrelationUID(applicationMessage.getCorrelationUID());

        PId destId = getHandlingApplication().getKoalaIdFactory().buildPIdFromHexString(applicationMessage.getSourceId());
        routeMessage(destId, responseApplicationMessage, null);
    }
}

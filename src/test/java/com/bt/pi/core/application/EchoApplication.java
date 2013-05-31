package com.bt.pi.core.application;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.payload.EchoPayload;

@Component
public class EchoApplication extends KoalaPastryApplicationBase {
    public static final String APPLICATION_NAME = "pi-echo-application";
    private static final int START_TIMEOUT_SEC = 10;
    private static final int ACTIVATION_CHECK_INTERVAL_SECS = 300;
    private static final Log LOG = LogFactory.getLog(EchoApplication.class);
    private ApplicationActivator applicationActivator;

    public EchoApplication() {
        applicationActivator = null;
    }

    @Resource(type = AlwaysOnApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        this.applicationActivator = anApplicationActivator;
    }

    @Override
    public boolean becomeActive() {
        MessageContext messageContext = newMessageContext();
        messageContext.routePiMessage(getKoalaIdFactory().convertToPId(getNodeHandle().getId()), EntityMethod.GET, new EchoPayload());
        LOG.debug("Started.");
        return true;
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext messageContext) {
        if (messageContext.getReceivedEntity() instanceof EchoPayload) {
            LOG.debug("EchoPayload message received.");
        }
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        System.err.println("Node: " + nodeId + " has left the ring.");

    }

    @Override
    public void becomePassive() {
        // TODO Auto-generated method stub

    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return ACTIVATION_CHECK_INTERVAL_SECS;
    }

    @Override
    public long getStartTimeout() {
        return START_TIMEOUT_SEC;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

}

package com.bt.pi.core.application;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.SuperNodeApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

public abstract class SuperNodeApplicationBase extends KoalaPastryScribeApplicationBase {
    private static final long DEFAULT_START_TIMEOUT_MILLIS = 30000;
    private static final int DEFAULT_ACTIVATION_CHECK_PERIOD_SECS = 60;

    private int activationCheckPeriodSecs;
    private long startTimeoutMillis;

    private ApplicationActivator applicationActivator;
    private PiLocation superNodeTopic;

    public SuperNodeApplicationBase() {
        activationCheckPeriodSecs = DEFAULT_ACTIVATION_CHECK_PERIOD_SECS;
        startTimeoutMillis = DEFAULT_START_TIMEOUT_MILLIS;
        applicationActivator = null;

        superNodeTopic = new PiLocation(getSuperNodeTopicUrl(), getSuperNodeTopicScope());
    }

    @Resource(type = SuperNodeApplicationActivator.class)
    public void setApplicationActivator(ApplicationActivator anApplicationActivator) {
        applicationActivator = anApplicationActivator;
    }

    protected PId getSuperNodeTopicId() {
        return getKoalaIdFactory().buildPId(superNodeTopic.getUrl()).forLocalScope(superNodeTopic.getNodeScope());
    }

    protected void setActivationCheckPeriodSecs(int value) {
        activationCheckPeriodSecs = value;
    }

    protected void setStartTimeoutMillis(long value) {
        startTimeoutMillis = value;
    }

    @Override
    public boolean becomeActive() {
        subscribe(superNodeTopic, this);
        return true;
    }

    @Override
    public void becomePassive() {
        unsubscribe(superNodeTopic, this);
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return activationCheckPeriodSecs;
    }

    @Override
    public long getStartTimeout() {
        return startTimeoutMillis;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.MILLISECONDS;
    }

    protected abstract NodeScope getSuperNodeTopicScope();

    protected abstract String getSuperNodeTopicUrl();

    @Override
    public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data) {
        throw new UnsupportedOperationException(String.format("Broadcast to %s is not yet supported", this.getApplicationName()));
    }

    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        throw new UnsupportedOperationException(String.format("Anycast to %s is not yet supported", this.getApplicationName()));
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        throw new UnsupportedOperationException(String.format("Directed message to %s is not yet supported", this.getApplicationName()));
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
        // TODO Should cause the activator to check and go active if needed.
    }
}

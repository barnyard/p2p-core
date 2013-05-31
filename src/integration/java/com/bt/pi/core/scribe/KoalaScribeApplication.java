package com.bt.pi.core.scribe;

import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rice.p2p.scribe.Topic;

import com.bt.pi.core.application.KoalaPastryScribeApplicationBase;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class KoalaScribeApplication extends KoalaPastryScribeApplicationBase {
    private int nodeNumber;
    private SubscribeDataReceivedListener listener;
    private CountDownLatch subscribeLatch;

    private ApplicationActivator applicationActivator;

    public KoalaScribeApplication(int nodeNumber, SubscribeDataReceivedListener listener, CountDownLatch subscribeLatch, CountDownLatch anycastFailureLatch) {
        this.nodeNumber = nodeNumber;
        this.listener = listener;
        this.subscribeLatch = subscribeLatch;

        applicationActivator = mock(ApplicationActivator.class);
    }

    @Override
    public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data) {
        listener.dataReceived(data, nodeNumber);
    }

    @Override
    public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
        listener.dataReceived(piEntity, nodeNumber);
        return true;
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public boolean becomeActive() {
        return false;
    }

    @Override
    public void becomePassive() {
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return 0;
    }

    @Override
    public String getApplicationName() {
        return "koala-test-app";
    }

    @Override
    public long getStartTimeout() {
        return 0;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return null;
    }

    @Override
    public void handleSubscribeSuccess(Collection<Topic> topics) {
        subscribeLatch.countDown();
    }
}

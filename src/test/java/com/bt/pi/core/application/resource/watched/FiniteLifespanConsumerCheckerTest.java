package com.bt.pi.core.application.resource.watched;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;

public class FiniteLifespanConsumerCheckerTest {
    private FiniteLifespanConsumerStatusChecker checker;
    private String consumerId;
    private PId consumerRecordId;
    private DhtClientFactory dhtClientFactory;
    private DhtReader dhtReader;
    private FiniteLifespanEntity entity;
    private GenericContinuationAnswer<PiEntity> readerAnswer;
    private FiniteLifespanConsumerCheckCallback<FiniteLifespanEntity> inactiveCallback;
    private FiniteLifespanConsumerCheckCallback<FiniteLifespanEntity> activeCallback;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        activeCallback = mock(FiniteLifespanConsumerCheckCallback.class);
        inactiveCallback = mock(FiniteLifespanConsumerCheckCallback.class);

        consumerId = "instance-id";
        consumerRecordId = mock(PId.class);
        entity = mock(FiniteLifespanEntity.class);

        dhtReader = mock(DhtReader.class);
        readerAnswer = new GenericContinuationAnswer<PiEntity>(entity);
        doAnswer(readerAnswer).when(dhtReader).getAsync(eq(consumerRecordId), isA(Continuation.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);

        checker = new FiniteLifespanConsumerStatusChecker(consumerId, consumerRecordId, dhtClientFactory);
    }

    @Test
    public void shouldCallBackActiveWhenInstanceAlive() {
        // setup
        when(entity.isDead()).thenReturn(false);

        // act
        checker.check(activeCallback, inactiveCallback);

        // assert
        verify(activeCallback).handleCallback(entity);
        verify(inactiveCallback, never()).handleCallback(any(FiniteLifespanEntity.class));
    }

    @Test
    public void shouldCallBackInactiveWhenInstanceDead() {
        // setup
        when(entity.isDead()).thenReturn(true);

        // act
        checker.check(activeCallback, inactiveCallback);

        // assert
        verify(activeCallback, never()).handleCallback(any(FiniteLifespanEntity.class));
        verify(inactiveCallback).handleCallback(entity);
    }
}

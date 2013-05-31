package com.bt.pi.core.application.resource.watched;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class FiniteLifespanConsumerStatusCheckerTest {
    @Mock
    private DhtClientFactory dhtClientFactory;
    private String consumerId = "consumerId";
    @Mock
    private PId consumerRecordId;
    @Mock
    private DhtReader reader;

    @SuppressWarnings("unchecked")
    @Test
    public void testCheckHandlesMissingDhtRecordGracefully() {
        // setup
        when(consumerRecordId.toStringFull()).thenReturn("myId");
        FiniteLifespanConsumerStatusChecker finiteLifespanConsumerStatusChecker = new FiniteLifespanConsumerStatusChecker(consumerId, consumerRecordId, dhtClientFactory);
        when(dhtClientFactory.createReader()).thenReturn(reader);
        FiniteLifespanConsumerCheckCallback<FiniteLifespanEntity> consumerInactiveCallback = mock(FiniteLifespanConsumerCheckCallback.class);
        FiniteLifespanConsumerCheckCallback<FiniteLifespanEntity> consumerActiveCallback = mock(FiniteLifespanConsumerCheckCallback.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation piContinuation = (PiContinuation) invocation.getArguments()[1];
                piContinuation.handleResult(null);
                return null;
            }
        }).when(reader).getAsync(eq(consumerRecordId), isA(PiContinuation.class));

        // act
        finiteLifespanConsumerStatusChecker.check(consumerActiveCallback, consumerInactiveCallback);

        // assert
        verify(consumerInactiveCallback).handleCallback(null);
    }
}

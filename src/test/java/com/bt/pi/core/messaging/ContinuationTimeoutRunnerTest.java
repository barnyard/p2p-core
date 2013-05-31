package com.bt.pi.core.messaging;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.exception.KoalaMessageTimeoutException;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.messaging.ContinuationTimeoutRunner;
import com.bt.pi.core.messaging.RequestState;
import com.bt.pi.core.messaging.RequestWrapperBase;

public class ContinuationTimeoutRunnerTest {

    private RequestState<Continuation<KoalaMessage, Exception>> requestState;
    private RequestWrapperBase<Continuation<KoalaMessage, Exception>, KoalaMessage> requestWrapper;
    private String correlationUUID;
    private ContinuationTimeoutRunner runner;
    private Continuation<KoalaMessage, Exception> continuation;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        requestState = mock(RequestState.class);
        requestWrapper = mock(RequestWrapperBase.class);
        continuation = mock(Continuation.class);
        correlationUUID = "correlationId";

        runner = new ContinuationTimeoutRunner(correlationUUID, requestState, requestWrapper);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRun() {
        // setup
        when(requestWrapper.awaitResponse(isA(RequestState.class))).thenReturn(false);
        when(requestState.getResponse()).thenReturn(continuation);

        // act
        runner.run();

        // verify
        verify(continuation).receiveException(isA(KoalaMessageTimeoutException.class));
    }
}

package com.bt.pi.core.past.continuation;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastContentHandle;
import rice.pastry.Id;

import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.past.content.KoalaContentHandleBase;
import com.bt.pi.core.past.continuation.KoalaFreshestHandleLookUpHandlesContinuation;

public class KoalaFreshestHandleLookUpHandlesContinuationTest {

    private KoalaFreshestHandleLookUpHandlesContinuation freshContinuation;
    private Past past;
    private Continuation<Object, Exception> appContinuation;
    PastContentHandle[] pastHandles;
    Environment env;
    TimeSource timeSource;
    PastContentHandle firstHandle;
    PastContentHandle middleHandle;
    PastContentHandle latestHandle;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        past = mock(Past.class);
        appContinuation = mock(Continuation.class);
        freshContinuation = new KoalaFreshestHandleLookUpHandlesContinuation(past, appContinuation);

        timeSource = mock(TimeSource.class);
        when(timeSource.currentTimeMillis()).thenReturn(System.currentTimeMillis()).thenReturn(System.currentTimeMillis() + 200000L).thenReturn(System.currentTimeMillis() + 400000L);

        env = mock(Environment.class);
        when(env.getTimeSource()).thenReturn(timeSource);

        firstHandle = new KoalaContentHandleBase(Id.build("1"), mock(NodeHandle.class), 1, env);
        middleHandle = new KoalaContentHandleBase(Id.build("2"), mock(NodeHandle.class), 1, env);
        latestHandle = new KoalaContentHandleBase(Id.build("3"), mock(NodeHandle.class), 1, env);
        pastHandles = new PastContentHandle[] { firstHandle, latestHandle, middleHandle };

    }

    @Test
    public void testReceiveResult() {

        // act
        freshContinuation.receiveResult(pastHandles);

        // verify
        verify(past).fetch(eq(firstHandle), eq(appContinuation));

    }

    /**
     * This should forward null when the handles are null.
     */
    @Test
    public void testReceiveResultWithNull() {
        // setup
        pastHandles = new PastContentHandle[] { null, null, null };

        // act
        freshContinuation.receiveResult(pastHandles);

        // verify
        verify(appContinuation).receiveResult(null);

    }

    @Test
    public void testReceiveException() {
        // setup
        Exception exception = new KoalaException("fooooo");

        // act
        freshContinuation.receiveException(exception);

        // verify
        verify(appContinuation).receiveException(eq(exception));

    }
}

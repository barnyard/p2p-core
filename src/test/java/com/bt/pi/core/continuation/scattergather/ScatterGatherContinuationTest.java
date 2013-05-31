package com.bt.pi.core.continuation.scattergather;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.dht.DhtOperationTimeoutException;

public class ScatterGatherContinuationTest {
    private CountDownLatch latch;
    private Continuation<Object, Exception> continuation;

    private ScatterGatherContinuation<Object, Continuation<Object, Exception>> scatterGatherContinuation;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        latch = new CountDownLatch(1);
        continuation = mock(Continuation.class);

        scatterGatherContinuation = new ScatterGatherContinuation<Object, Continuation<Object, Exception>>(continuation);
        scatterGatherContinuation.setLatch(latch);
    }

    @Test
    public void testGetParentContinuation() throws Exception {
        // setup

        // act
        Continuation<Object, Exception> parentContinuation = scatterGatherContinuation.getParentContinuation();

        // assert
        assertThat(continuation, equalTo(parentContinuation));
    }

    @Test
    public void testReceiveResult() throws Exception {
        // setup
        Object object = new Object();

        // act
        scatterGatherContinuation.receiveResult(object);

        // assert
        assertThat(latch.await(1, TimeUnit.NANOSECONDS), is(true));
        verify(continuation).receiveResult(object);
    }

    @Test
    public void testReceiveException() throws Exception {
        // setup
        Exception exception = new Exception();

        // act
        scatterGatherContinuation.receiveException(exception);

        // assert
        assertThat(latch.await(1, TimeUnit.NANOSECONDS), is(true));
        verify(continuation).receiveException(exception);
    }

    @Test
    public void testParentContinuationReceiveResultThrowsExceptionLatchShouldStillCountdown() throws InterruptedException {
        // setup
        doThrow(new DhtOperationTimeoutException("blah!")).when(continuation).receiveResult(anyObject());

        // act
        scatterGatherContinuation.receiveResult("bob");

        // assert
        assertThat(latch.await(1, TimeUnit.NANOSECONDS), is(true));
    }

    @Test
    public void testParentContinuationReceiveExceptionThrowsExceptionLatchShouldStillCountdown() throws InterruptedException {
        // setup
        doThrow(new DhtOperationTimeoutException("blah!")).when(continuation).receiveException(isA(Exception.class));

        // act
        scatterGatherContinuation.receiveException(new Exception());

        // assert
        assertThat(latch.await(1, TimeUnit.NANOSECONDS), is(true));
    }
}

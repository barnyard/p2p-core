package com.bt.pi.core.continuation.scattergather;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.continuation.GenericContinuation;

public class ScatterGatherContinuation<T, C extends Continuation<T, Exception>> extends GenericContinuation<T> {
    private static final Log LOG = LogFactory.getLog(ScatterGatherContinuation.class);
    private CountDownLatch latch;
    private C parentContinuation;

    public ScatterGatherContinuation(C aParentContinuation) {
        latch = null;
        parentContinuation = aParentContinuation;
    }

    @Override
    public void handleException(Exception exception) {
        try {
            parentContinuation.receiveException(exception);
        } catch (Throwable t) {
            LOG.warn(String.format("Exception while trying to receiveException %s on parent continuation. Counting down on latch.", exception), t);
        }
        latch.countDown();
    }

    @Override
    public void handleResult(T result) {
        try {
            parentContinuation.receiveResult(result);
        } catch (Throwable t) {
            LOG.warn(String.format("Exception while trying to receiveResult %s on parent continuation. Counting down on latch.", result), t);
        }
        latch.countDown();
    }

    protected C getParentContinuation() {
        return parentContinuation;
    }

    public void setLatch(CountDownLatch aLatch) {
        latch = aLatch;
    }
}

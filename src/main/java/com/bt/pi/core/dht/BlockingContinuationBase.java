//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.dht;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.continuation.GenericContinuation;

/**
 * This PAST based continuation blocks until it gets it's result and then counts down on a latch that's passed in the
 * constructor
 */
public abstract class BlockingContinuationBase<T> extends GenericContinuation<T> {
    private static final Log LOG = LogFactory.getLog(BlockingContinuationBase.class);
    private static final int DEFAULT_TIMEOUT_SECS = 30;
    private CountDownLatch latch;
    private int blockingTimeoutSecs;
    private Exception exception;
    private T result;

    public BlockingContinuationBase() {
        this(DEFAULT_TIMEOUT_SECS);
    }

    public BlockingContinuationBase(int aBlockingTimeoutSecs) {
        latch = new CountDownLatch(1);
        exception = null;
        result = null;
        blockingTimeoutSecs = aBlockingTimeoutSecs;
    }

    public Exception getException() {
        return exception;
    }

    public T getResult() {
        return result;
    }

    @Override
    public void handleResult(T aResult) {
        LOG.debug(String.format("receieveResult(%s)", aResult));
        result = aResult;
        latch.countDown();
    }

    @Override
    public void handleException(Exception ex) {
        LOG.debug(String.format("Received exception: %s", ex.getMessage()));
        exception = ex;
        latch.countDown();
    }

    public T blockUntilComplete() {
        LOG.debug("blockUntilComplete ");
        boolean completed;
        try {
            completed = this.latch.await(blockingTimeoutSecs, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!completed) {
            DhtOperationTimeoutException dhtOperationTimeoutException = new DhtOperationTimeoutException(String.format("DHT get did not complete in %d seconds", blockingTimeoutSecs));
            LOG.error("Throwing exception: " + dhtOperationTimeoutException.getMessage());
            throw dhtOperationTimeoutException;
        }

        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        } else {
            LOG.debug("returning from blockUntilComplete. Result: " + result);
            return result;
        }
    }
}

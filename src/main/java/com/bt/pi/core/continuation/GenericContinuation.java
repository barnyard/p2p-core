package com.bt.pi.core.continuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class GenericContinuation<T> extends LoggingContinuation<T> {
    private static final Log LOG = LogFactory.getLog(GenericContinuation.class);

    public GenericContinuation() {
    }

    @Override
    public void receiveExceptionInternal(final Exception exception) {
        try {
            handleException(exception);
        } catch (Throwable t) {
            LOG.error(String.format("Caught exception whilst executing exception handler: %s", t.getMessage()), t);
        }
    }

    @Override
    public void receiveResultInternal(final T result) {
        try {
            handleResult(result);
        } catch (Exception e) {
            receiveException(e);
        }
    }

    public void handleException(final Exception e) {
    }

    public abstract void handleResult(final T result);
}
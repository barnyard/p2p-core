package com.bt.pi.core.past.continuation;

import rice.Continuation;

import com.bt.pi.core.continuation.ContinuationUtils;

public abstract class LoggingContinuationWithKoalaHandleVersionComparer<T> extends KoalaHandleVersionComparer implements Continuation<T, Exception> {
    public LoggingContinuationWithKoalaHandleVersionComparer() {
    }

    @Override
    public final void receiveException(Exception exception) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, null);
        try {
            receiveExceptionInternal(exception);
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, null, startTime);
        }

    }

    public final void receiveResult(T result) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_RESULT, null);
        try {
            receiveResultInternal(result);
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_RESULT, null, startTime);
        }
    }

    protected abstract void receiveExceptionInternal(Exception exception);

    protected abstract void receiveResultInternal(T result);
}

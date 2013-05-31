package com.bt.pi.core.continuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.exception.KoalaContentVersionMismatchException;
import com.bt.pi.core.util.MDCHelper;

public class LoggingContinuation<T> implements Continuation<T, Exception> {
    private static final Log LOG = LogFactory.getLog(LoggingContinuation.class);
    private String transactionUID;

    public LoggingContinuation() {
        transactionUID = MDCHelper.getTransactionUID();
    }

    @Override
    public final void receiveException(final Exception exception) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, transactionUID);

        try {
            MDCHelper.putTransactionUID(transactionUID);
            if (exception instanceof KoalaContentVersionMismatchException) {
                LOG.info(String.format("Exception during async call caused by version mismatch: %s", exception.getMessage()), exception);
            } else {
                LOG.error(String.format("Exception during async call: %s", exception == null ? "null exception object" : exception.getMessage()), exception);
            }
            receiveExceptionInternal(exception);
            MDCHelper.clearTransactionUID();
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, transactionUID, startTime);
        }
    }

    protected void receiveExceptionInternal(Exception exception) {
    }

    @Override
    public final void receiveResult(final T result) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_RESULT, transactionUID);

        try {
            MDCHelper.putTransactionUID(transactionUID);

            if (LOG.isDebugEnabled())
                LOG.debug(String.format("%s(%s)", ContinuationUtils.RECEIVE_RESULT, result));

            receiveResultInternal(result);
            MDCHelper.clearTransactionUID();
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_RESULT, transactionUID, startTime);
        }
    }

    protected void receiveResultInternal(T result) {
    }
}
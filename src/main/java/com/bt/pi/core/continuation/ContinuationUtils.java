package com.bt.pi.core.continuation;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ContinuationUtils {
    public static final String RECEIVE_RESULT = "receiveResult";
    public static final String RECEIVE_EXCEPTION = "receiveException";
    public static final String APPLICATION_UPDATE = "application.update";
    public static final String APPLICATION_DELIVER = "application.deliver";
    public static final String APPLICATION_FORWARD = "application.forward";
    public static final String SCRIBE_DELIVER = "scribe.deliver";
    public static final String SCRIBE_ANYCAST = "scribe.anycast";
    public static final String SCRIBE_SUBSCRIBE_SUCCESS = "scribe.subscribe.success";
    public static final String SCRIBE_SUBSCRIBE_FAILED = "scribe.subscribe.failed";
    public static final String SCRIBE_CHILD_ADDED = "scribe.child.added";
    public static final String SCRIBE_CHILD_REMOVED = "scribe.child.removed";

    private static final int THREE_SECONDS = 3000;
    private static final int SIX_SECONDS = 6000;
    private static final int TEN_SECONDS = 10000;
    private static final String START_S_S = "START: %s(): %s";
    private static final String END_S_S_DURATION_D_MILLIS = "END: %s(): %s, DURATION: %d millis";

    private static final Log LOG = LogFactory.getLog(ContinuationUtils.class);

    protected ContinuationUtils() {
    }

    public static long logStartOfContinuation(String methodName, String transactionUID) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(START_S_S, methodName, transactionUID));
        return System.currentTimeMillis();
    }

    public static void logEndOfContinuation(String methodName, String transactionUID, long startTime) {
        long durationInMillis = System.currentTimeMillis() - startTime;
        if (durationInMillis < THREE_SECONDS) {
            if (LOG.isDebugEnabled())
                LOG.debug(String.format(END_S_S_DURATION_D_MILLIS, methodName, transactionUID, durationInMillis));
        } else if (durationInMillis < SIX_SECONDS)
            LOG.info(String.format(END_S_S_DURATION_D_MILLIS, methodName, transactionUID, durationInMillis));
        else if (durationInMillis < TEN_SECONDS) {
            LOG.warn(String.format(END_S_S_DURATION_D_MILLIS, methodName, transactionUID, durationInMillis));
            LOG.warn(Arrays.toString(Thread.currentThread().getStackTrace()));
        } else {
            LOG.error(String.format(END_S_S_DURATION_D_MILLIS, methodName, transactionUID, durationInMillis));
            LOG.error(Arrays.toString(Thread.currentThread().getStackTrace()));
        }
    }
}

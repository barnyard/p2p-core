package com.bt.pi.core.util;

import org.apache.log4j.MDC;

public final class MDCHelper {
    public static final String MDC_TRANSACTION_ID = "TRANSACTION_ID";

    private MDCHelper() {
    }

    public static String getTransactionUID() {
        return (String) MDC.get(MDC_TRANSACTION_ID);
    }

    public static void putTransactionUID(String transactionUID) {
        if (transactionUID != null)
            MDC.put(MDC_TRANSACTION_ID, transactionUID);
    }

    public static void clearTransactionUID() {
        MDC.remove(MDC_TRANSACTION_ID);
    }
}

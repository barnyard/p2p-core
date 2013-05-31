package com.bt.pi.core.application;

import java.util.UUID;

import com.bt.pi.core.util.MDCHelper;

public class MessageContextBase implements TransactionAwareContext {
    private KoalaPastryApplicationBase handlingApp;
    private String transactionUID;

    protected MessageContextBase(KoalaPastryApplicationBase aHandlingApp) {
        this(aHandlingApp, null);
    }

    protected MessageContextBase(KoalaPastryApplicationBase aHandlingApp, String aTransactionUID) {
        handlingApp = aHandlingApp;
        if (aTransactionUID != null)
            setTransactionUID(aTransactionUID);
        else
            setTransactionUID(UUID.randomUUID().toString());
    }

    @Override
    public String getTransactionUID() {
        return transactionUID;
    }

    public void setTransactionUID(String aTransactionUID) {
        this.transactionUID = aTransactionUID;
        MDCHelper.putTransactionUID(transactionUID);
        // TODO: figure out when / how to remove this transaction uid
    }

    public KoalaPastryApplicationBase getHandlingApplication() {
        return handlingApp;
    }
}

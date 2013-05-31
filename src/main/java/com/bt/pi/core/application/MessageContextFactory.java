package com.bt.pi.core.application;

public interface MessageContextFactory {
    MessageContext newMessageContext();

    MessageContext newMessageContext(String transactionUID);
}

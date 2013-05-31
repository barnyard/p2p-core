package com.bt.pi.core.application.resource.watched;


public interface FiniteLifespanConsumerCheckCallback<T extends FiniteLifespanEntity> {
    void handleCallback(T consumerEntity);
}

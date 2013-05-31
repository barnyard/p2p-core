package com.bt.pi.core.application.resource;

import java.util.HashSet;
import java.util.Set;

public class ConsumedResourceState<K> {
    protected static final long DEFAULT_POLL_INTERVAL = -1;
    private K id;
    private Set<String> consumerSet;

    protected ConsumedResourceState(K aId) {
        consumerSet = new HashSet<String>();
        id = aId;
    }

    public K getId() {
        return id;
    }

    protected Set<String> getConsumerSet() {
        return consumerSet;
    }

    public boolean registerConsumer(String consumerId) {
        consumerSet.add(consumerId);
        return consumerSet.size() <= 1;
    }

    public boolean deregisterConsumer(String consumerId) {
        consumerSet.remove(consumerId);
        return consumerSet.isEmpty();
    }
}

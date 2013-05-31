package com.bt.pi.core.application.resource;

public class CachedConsumedResourceState<K> extends ConsumedResourceState<K> {
    protected static final long DEFAULT_POLL_INTERVAL = -1;
    private Object entity;

    protected CachedConsumedResourceState(K aId, Object aEntity) {
        super(aId);
        this.entity = aEntity;
    }

    @SuppressWarnings("unchecked")
    public <T> T getEntity() {
        return (T) entity;
    }

    public <T> void setEntity(T aEntity) {
        this.entity = aEntity;
    }
}

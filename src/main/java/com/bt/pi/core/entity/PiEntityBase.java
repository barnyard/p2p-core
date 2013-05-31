package com.bt.pi.core.entity;

/**
 * The base implementation for all PiEntity objects.
 * 
 */
public abstract class PiEntityBase implements PiEntity {
    private long version;

    public PiEntityBase() {
        version = 0;
    }

    @Override
    public void incrementVersion() {
        version++;
    };

    @Override
    public long getVersion() {
        return version;
    };

    public void setVersion(long dataVersion) {
        version = dataVersion;
    }
}

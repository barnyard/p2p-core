package com.bt.pi.core.past.content;

import rice.p2p.past.gc.GCPastMetadata;

public class KoalaGCPastMetadata extends GCPastMetadata {
    private static final long serialVersionUID = 8430651051393333460L;

    private boolean deletedAndDeletable;
    private String entityType;

    public KoalaGCPastMetadata(long version, boolean isDeletedAndDeletable, String anEntityType) {
        super(version);
        deletedAndDeletable = isDeletedAndDeletable;
        entityType = anEntityType;
    }

    public boolean isDeletedAndDeletable() {
        return deletedAndDeletable;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s[version: %s, deletedAndDeletable: %s, entityType: %s]", getClass().getSimpleName(), getExpiration(), deletedAndDeletable, entityType);
    }
}

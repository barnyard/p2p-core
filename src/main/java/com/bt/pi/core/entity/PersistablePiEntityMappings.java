package com.bt.pi.core.entity;

import java.util.Collection;

public class PersistablePiEntityMappings {
    private Collection<PersistablePiEntityMapping> persistablePiEntityMappings;

    public PersistablePiEntityMappings() {
        persistablePiEntityMappings = null;
    }

    public void setPersistablePiEntityMappings(Collection<PersistablePiEntityMapping> aPersistablePiEntityMappings) {
        persistablePiEntityMappings = aPersistablePiEntityMappings;
    }

    public Collection<PersistablePiEntityMapping> getPersistablePiEntityMappings() {
        return persistablePiEntityMappings;
    }
}

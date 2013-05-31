/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.entity;

import java.util.Collection;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnore;

public abstract class PiEntityCollection<T extends PiEntity> extends PiEntityBase {
    private Collection<T> entities;

    public PiEntityCollection() {
        entities = null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    public void setEntities(Collection<T> ents) {
        entities = ents;
    }

    public Collection<T> getEntities() {
        return entities;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (null == obj)
            return false;
        if (!(obj instanceof PiEntityCollection))
            return false;

        return (entities == null && ((PiEntityCollection) obj).getEntities() == null) || this.entities.equals(((PiEntityCollection) obj).getEntities());
    }

    @Override
    public String toString() {
        if (null != entities)
            return String.format("%s, size = %d", super.toString(), entities.size());
        return super.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(entities).toHashCode();
    }

    @JsonIgnore
    @Override
    public long getVersion() {
        return super.getVersion();
    }
}

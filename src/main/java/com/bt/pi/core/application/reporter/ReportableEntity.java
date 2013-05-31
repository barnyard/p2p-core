/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.reporter;

import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.util.collections.ConcurrentSortedBoundQueueElement;

public abstract class ReportableEntity<T> extends PiEntityBase implements ConcurrentSortedBoundQueueElement<T>, Comparable<T> {
    private String theNodeId;

    public ReportableEntity(String nodeId) {
        this.theNodeId = nodeId;
    }

    public ReportableEntity() {
        this.theNodeId = null;
    }

    public String getNodeId() {
        return theNodeId;
    }

    public void setNodeId(String nodeId) {
        this.theNodeId = nodeId;
    }

    public abstract boolean equals(Object other);

    public abstract int hashCode();

    public abstract long getCreationTime();
}

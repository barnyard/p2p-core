package com.bt.pi.core.application.resource.leased;

import com.bt.pi.core.entity.PiEntity;

public interface LeasedResourceAllocationRecord<ResourceIdType> extends PiEntity {
    boolean heartbeat(ResourceIdType resourceId, String consumerId);
}

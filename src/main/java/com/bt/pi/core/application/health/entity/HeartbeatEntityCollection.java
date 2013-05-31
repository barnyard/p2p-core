package com.bt.pi.core.application.health.entity;

import com.bt.pi.core.entity.PiEntityCollection;

public class HeartbeatEntityCollection extends PiEntityCollection<HeartbeatEntity> {
    public HeartbeatEntityCollection() {
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}

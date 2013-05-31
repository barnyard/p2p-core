package com.bt.pi.core.application.resource.watched;

import com.bt.pi.core.entity.PiEntity;

public interface FiniteLifespanEntity extends PiEntity {
    boolean isDead();
}

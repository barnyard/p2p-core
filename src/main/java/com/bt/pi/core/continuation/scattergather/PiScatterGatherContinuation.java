package com.bt.pi.core.continuation.scattergather;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.PiEntity;

public class PiScatterGatherContinuation<T extends PiEntity> extends ScatterGatherContinuation<T, PiContinuation<T>> {
    public PiScatterGatherContinuation(PiContinuation<T> parentContinuation) {
        super(parentContinuation);
    }
}

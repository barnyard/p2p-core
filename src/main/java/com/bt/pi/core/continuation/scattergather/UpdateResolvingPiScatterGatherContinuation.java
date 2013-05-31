package com.bt.pi.core.continuation.scattergather;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.PiEntity;

public class UpdateResolvingPiScatterGatherContinuation<T extends PiEntity> extends ScatterGatherContinuation<T, UpdateResolvingPiContinuation<T>> implements UpdateResolvingContinuation<T, Exception> {
    public UpdateResolvingPiScatterGatherContinuation(UpdateResolvingPiContinuation<T> parentContinuation) {
        super(parentContinuation);
    }

    @Override
    public T update(T existingEntity, T requestedEntity) {
        return getParentContinuation().update(existingEntity, requestedEntity);
    }
}

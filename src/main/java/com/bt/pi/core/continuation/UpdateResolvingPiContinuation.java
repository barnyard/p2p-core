package com.bt.pi.core.continuation;

import com.bt.pi.core.entity.PiEntity;

public abstract class UpdateResolvingPiContinuation<T extends PiEntity> extends PiContinuation<T> implements UpdateResolvingContinuation<T, Exception> {
    public UpdateResolvingPiContinuation() {
    }
}

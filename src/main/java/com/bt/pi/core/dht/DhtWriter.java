package com.bt.pi.core.dht;

import rice.Continuation;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/**
 * Interface for all DHT writers.
 * 
 * 
 */
public interface DhtWriter {
    /**
     * Asynchronous unconditional put operation
     * 
     * @param id
     * @param entity
     * @param continuation
     */
    <T extends PiEntity> void put(final PId id, final T entity, Continuation<T, Exception> continuation);

    /**
     * Asynchronous update operation. Content to be written can be compared to existing content against the same id, and
     * altered as required, via updateResolver.
     * 
     * @param id
     * @param entity
     * @param updateResolvingContinuation
     */
    <T extends PiEntity> void update(final PId id, final T entity, final UpdateResolvingContinuation<T, Exception> updateResolvingContinuation);

    /**
     * Asynchronous update operation.
     * 
     * @param id
     * @param updateResolvingContinuation
     */
    <T extends PiEntity> void update(final PId id, final UpdateResolvingContinuation<T, Exception> updateResolvingContinuation);
}

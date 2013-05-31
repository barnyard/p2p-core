package com.bt.pi.core.dht;

import rice.Continuation;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/**
 * Interface for all DHT readers.
 * 
 */
public interface DhtReader {
    /**
     * Asynchronous get operation
     * 
     * @param id
     * @param continuation
     */
    <T extends PiEntity> void getAsync(final PId id, Continuation<T, Exception> continuation);

    /**
     * Asynchronous get operation that gets the nearest copy of the data. There are no guarantees on the correctness of
     * the data retrieved.
     * 
     * @param id
     * @param continuation
     */
    <T extends PiEntity> void getAnyAsync(final PId id, Continuation<T, Exception> continuation);
}

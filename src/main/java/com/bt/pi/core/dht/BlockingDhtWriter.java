package com.bt.pi.core.dht;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public interface BlockingDhtWriter extends DhtClient {
    /**
     * Blocking unconditional put operation
     * 
     * @param id
     * @param entity
     */
    <T extends PiEntity> void put(final PId id, final T entity);

    /**
     * This is a blocking write which returns if the entity was written. The entity will only be written if it is not
     * present.
     * 
     * @param id
     * @param piEntity
     */

    boolean writeIfAbsent(final PId id, final PiEntity piEntity);

    /**
     * Blocking update operation. Content to be written can be compared to existing content against the same id, and
     * altered as required, via updateResolver.
     * 
     * @param id
     * @param entity
     * @param updateResolver
     */
    <T extends PiEntity> void update(final PId id, final T entity, final UpdateResolver<T> updateResolver);

    /**
     * Returns the object successfully written to the DHT, or null if write failed or was abandoned
     */
    PiEntity getValueWritten();
}

package com.bt.pi.core.continuation;

public interface UpdateResolver<T> {
    /**
     * The update method is called as a result of the DHT read to retrieve the existing entity. Because a DHT write may
     * fail due to version conflict this update method should be written to deal with multiple calls.
     * 
     * @param existingEntity
     * @param requestedEntity
     * @return
     */
    T update(T existingEntity, T requestedEntity);
}

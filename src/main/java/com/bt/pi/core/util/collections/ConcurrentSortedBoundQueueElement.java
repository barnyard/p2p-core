package com.bt.pi.core.util.collections;


public interface ConcurrentSortedBoundQueueElement<T> extends Comparable<T> {
    int getKeysForMapCount();

    Object[] getKeysForMap();
}

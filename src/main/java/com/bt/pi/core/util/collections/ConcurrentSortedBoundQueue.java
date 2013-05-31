package com.bt.pi.core.util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ConcurrentSortedBoundQueue<T extends ConcurrentSortedBoundQueueElement<?>> extends ConcurrentSkipListSet<T> {
    private static final long serialVersionUID = 3550334257716147928L;

    private int maxSize;
    private ArrayList<ConcurrentMap<Object, Collection<T>>> maps; // List isn't serializable and that causes issues

    public ConcurrentSortedBoundQueue(int aMaxSize) {
        super();
        maxSize = aMaxSize;
        maps = new ArrayList<ConcurrentMap<Object, Collection<T>>>();
    }

    @Override
    public boolean add(T e) {

        if (maxSize <= 0)
            return false;

        checkElementKeysAreNotNullOrEmpty(e);

        while (size() >= maxSize) {
            if (floor(e) == null)
                return false;

            remove(first());
        }

        if (super.add(e)) {
            if (maps == null)
                return true;

            if (maps.isEmpty())
                createNewMaps(e);

            if (!maps.isEmpty())
                populateMaps(e);

            return true;
        }
        return false;
    }

    private void checkElementKeysAreNotNullOrEmpty(T e) {
        for (Object key : e.getKeysForMap()) {
            if (key == null || (key instanceof String && StringUtils.isEmpty((String) key)))
                throw new IllegalArgumentException("Tried to add an element containing an null key");
        }
    }

    private void populateMaps(T e) {
        Object[] keys = e.getKeysForMap();
        for (int i = 0; i < keys.length; i++) {
            ConcurrentMap<Object, Collection<T>> concurrentMap = maps.get(i);
            Collection<T> newCollection = new ArrayList<T>();
            Collection<T> existingCollection = concurrentMap.putIfAbsent(keys[i], newCollection);

            Collection<T> collection = existingCollection == null ? newCollection : existingCollection;
            collection.add(e);
        }
    }

    private void createNewMaps(T e) {
        for (int i = 0; i < e.getKeysForMapCount(); i++)
            maps.add(new ConcurrentHashMap<Object, Collection<T>>());
    };

    @Override
    public void clear() {
        for (ConcurrentMap<Object, Collection<T>> map : maps)
            map.clear();
    }

    @Override
    public boolean remove(Object o) {
        if (!super.remove(o))
            return false;

        if (!(o instanceof ConcurrentSortedBoundQueueElement<?>))
            return true;

        Object[] keysForMap = ((ConcurrentSortedBoundQueueElement<?>) o).getKeysForMap();
        if (keysForMap == null)
            return true;

        for (int i = 0; i < keysForMap.length; i++) {
            Collection<T> collection = maps.get(i).get(keysForMap[i]);
            if (collection != null) {
                collection.remove(o);
                if (collection.isEmpty())
                    maps.get(i).remove(keysForMap[i]);
            }
        }
        return true;
    }

    public Collection<T> getCollectionByKey(int index, Object key) {
        return maps.get(index).get(key);
    }

    private void trimToSize() {
        while (size() > maxSize)
            remove(first());
    }

    public void setMaxSize(int aMaxSize) {
        maxSize = aMaxSize;
        trimToSize();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConcurrentSortedBoundQueue<?>))
            return false;

        return maxSize == ((ConcurrentSortedBoundQueue<?>) o).maxSize && super.equals(o);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(maxSize).toHashCode();
    }

    public void setSize(int newMaxSize) {
        maxSize = newMaxSize;
        trimToSize();
    }
}

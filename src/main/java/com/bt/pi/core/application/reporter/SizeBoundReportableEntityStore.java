package com.bt.pi.core.application.reporter;

import java.util.Collection;
import java.util.Iterator;

import com.bt.pi.core.util.collections.ConcurrentSortedBoundQueue;

public class SizeBoundReportableEntityStore<T extends ReportableEntity<?>> implements ReportableEntityStore<T> {
    private ConcurrentSortedBoundQueue<T> entityStore;

    public SizeBoundReportableEntityStore(int size) {
        entityStore = new ConcurrentSortedBoundQueue<T>(size);
    }

    @Override
    public void add(T entity) {
        entityStore.add(entity);
    }

    @Override
    public void addAll(Collection<T> entities) {
        entityStore.addAll(entities);
    }

    @Override
    public Iterator<T> getPublishIterator() {
        return entityStore.descendingIterator();
    }

    @Override
    public Collection<T> getAllEntities() {
        return entityStore;
    }

    @Override
    public boolean exists(ReportableEntity<?> entity) {
        Iterator<T> iterator = getPublishIterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (entity.equals(next))
                return true;
        }
        return false;
    }
}

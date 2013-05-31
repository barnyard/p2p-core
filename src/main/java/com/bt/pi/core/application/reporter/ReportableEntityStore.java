package com.bt.pi.core.application.reporter;

import java.util.Collection;
import java.util.Iterator;

public interface ReportableEntityStore<T extends ReportableEntity<?>> {
    void add(T entity);

    void addAll(Collection<T> entities);

    Iterator<T> getPublishIterator();

    Collection<T> getAllEntities();

    boolean exists(ReportableEntity<?> entity);
}

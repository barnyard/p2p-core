package com.bt.pi.core.application.reporter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NodeBasedReportableEntityStore<T extends ReportableEntity<?>> implements ReportableEntityStore<T> {
    private Map<String, T> nodeMap;

    public NodeBasedReportableEntityStore() {
        nodeMap = new ConcurrentHashMap<String, T>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(T entity) {
        if (nodeMap.containsKey(entity.getNodeId())) {
            Comparable<T> existingEntity = (Comparable<T>) nodeMap.get(entity.getNodeId());
            if (existingEntity.compareTo(entity) > 0)
                return;
        }
        nodeMap.put(entity.getNodeId(), entity);
    }

    @Override
    public void addAll(Collection<T> entities) {
        Iterator<T> iterator = entities.iterator();
        while (iterator.hasNext())
            add(iterator.next());
    }

    @Override
    public Collection<T> getAllEntities() {
        return nodeMap.values();
    }

    @Override
    public Iterator<T> getPublishIterator() {
        return Collections.unmodifiableCollection(nodeMap.values()).iterator();
    }

    public T getByNodeId(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public boolean exists(ReportableEntity<?> entity) {
        return existsAndNewer(entity);
    }

    // we don't want to replace a newer version of the entity with an older one
    private boolean existsAndNewer(ReportableEntity<?> entity) {
        T t = nodeMap.get(entity.getNodeId());
        if (null == t)
            return false;
        return t.getCreationTime() >= entity.getCreationTime();
    }
}

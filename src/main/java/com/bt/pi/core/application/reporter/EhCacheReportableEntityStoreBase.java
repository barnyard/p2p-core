package com.bt.pi.core.application.reporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

public abstract class EhCacheReportableEntityStoreBase<T extends TimeBasedReportableEntity<?>> {
    private static final Log LOG = LogFactory.getLog(EhCacheReportableEntityStoreBase.class);
    private static final String ERROR_CREATING_A_CACHE_INSTANCE = "Error creating a cache instance";

    private EhCacheFactoryBean ehCacheFactoryBean;
    private Cache cache;

    public EhCacheReportableEntityStoreBase(String name, int timeToIdleInSeconds, int timeToLiveInSeconds) {
        cache = null;

        ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheName(name);
        ehCacheFactoryBean.setOverflowToDisk(false);
        ehCacheFactoryBean.setTimeToIdle(timeToIdleInSeconds);
        ehCacheFactoryBean.setTimeToLive(timeToLiveInSeconds);
    }

    protected EhCacheFactoryBean getEhCacheFactoryBean() {
        return ehCacheFactoryBean;
    }

    protected void initEhCache() {
        try {
            ehCacheFactoryBean.afterPropertiesSet();
        } catch (CacheException e) {
            LOG.error(ERROR_CREATING_A_CACHE_INSTANCE, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error(ERROR_CREATING_A_CACHE_INSTANCE, e);
            throw new RuntimeException(e);
        }

        cache = (Cache) ehCacheFactoryBean.getObject();
    }

    public void removeCache() {
        String cacheName = cache.getName();
        if (CacheManager.getInstance().cacheExists(cacheName))
            CacheManager.getInstance().removeCache(cacheName);
    }

    /*
     * This method sets the cache creation time to the entity creation time
     * @see com.bt.pi.core.application.reporter.ReportableEntityStore#add(com.bt.pi.core.application.reporter.ReportableEntity)
     */
    public void add(T entity) {
        cache.put(new Element(entity.getId(), entity, 1, entity.getCreationTime(), entity.getCreationTime(), entity.getCreationTime(), 1));
    }

    /*
     * This method sets the cache creation time to the entry creation time for all the entities in the collection.
     * @see com.bt.pi.core.application.reporter.ReportableEntityStore#addAll(java.util.Collection)
     */
    public void addAll(Collection<T> entities) {
        if (entities != null && !entities.isEmpty()) {
            for (T entity : entities)
                add(entity);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getAllEntities() {
        Collection<T> allEntities = new ArrayList<T>();
        for (Object key : cache.getKeysWithExpiryCheck()) {
            allEntities.add((T) cache.get(key).getObjectValue());
        }
        return allEntities;
    }

    public Iterator<T> getPublishIterator() {
        return getAllEntities().iterator();
    }

    public boolean exists(ReportableEntity<?> entity) {
        return existsAndNewer(entity);
    }

    // we don't want to replace a newer version of the entity with an older one
    private boolean existsAndNewer(ReportableEntity<?> entity) {
        if (entity instanceof TimeBasedReportableEntity<?>) {
            TimeBasedReportableEntity<?> timeBasedReportableEntity = (TimeBasedReportableEntity<?>) entity;
            Element cacheElement = cache.get(timeBasedReportableEntity.getId());
            if (cacheElement == null)
                return false;
            timeBasedReportableEntity = (TimeBasedReportableEntity<?>) cacheElement.getObjectValue();
            return timeBasedReportableEntity != null && timeBasedReportableEntity.getCreationTime() > entity.getCreationTime();
        }
        return false;
    }
}

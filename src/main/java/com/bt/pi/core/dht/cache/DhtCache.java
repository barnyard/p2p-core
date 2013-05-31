package com.bt.pi.core.dht.cache;

import javax.annotation.Resource;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/**
 * The DhtCache can be used to add caching functionality into the DHT reads and writes.
 * 
 * Note: {@link BlockingDhtCache} may use a different cache store depending upon applicaitonContext setup. By default
 * only 100 items will be kept in the cache at one time. Additionally, items will be cached for 10 minutes by default.
 */
public class DhtCache {
    private static final String DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION = "Delegating handling of exception %s (%s) to calling continuation";
    private static final String UNCHECKED = "unchecked";
    private static final Log LOG = LogFactory.getLog(DhtCache.class);
    private Cache cache;
    private DhtClientFactory dhtClientFactory;

    public DhtCache() {
        dhtClientFactory = null;
        cache = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    protected Cache getCache() {
        return cache;
    }

    public void setCache(Cache aCache) {
        this.cache = aCache;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PiEntity> void get(final PId id, final PiContinuation<T> continuation) {
        LOG.debug(String.format("get(%s, %s)", id, continuation));

        Element e = this.cache.get(id);
        if (e != null) {
            LOG.debug(String.format("Found %s in cache", id.toStringFull()));
            continuation.receiveResult((T) e.getObjectValue());
        } else {
            LOG.debug(String.format("%s not in cache, reading from the dht", id.toStringFull()));
            getReadThrough(id, continuation);
        }
    }

    public <T extends PiEntity> void getReadThrough(final PId id, final GenericContinuation<T> continuation) {
        LOG.debug(String.format("getReadThrough(%s, %s)", id.toStringFull(), continuation));

        DhtReader dhtReader = dhtClientFactory.createReader();
        dhtReader.getAsync(id, new PiContinuation<T>() {
            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format(DELEGATING_HANDLING_OF_EXCEPTION_S_S_TO_CALLING_CONTINUATION, e.getClass().getName(), e.getMessage()));
                continuation.receiveException(e);
            }

            @Override
            public void handleResult(T result) {
                LOG.debug(String.format("get from dht for id %s returned %s", id.toStringFull(), result));
                T entityToReturn = updateCacheWithEntityReadFromDhtAndReturnFreshest(id, result);
                continuation.receiveResult(entityToReturn);
            }
        });
    }

    public <T extends PiEntity> void update(final PId id, final UpdateResolvingContinuation<T, Exception> updateResolvingContinuation) {
        update(id, null, updateResolvingContinuation);
    }

    public <T extends PiEntity> void update(final PId id, final T requestedEntity, final UpdateResolvingContinuation<T, Exception> updateResolvingContinuation) {
        LOG.debug(String.format("update(%s, %s)", id.toStringFull(), updateResolvingContinuation));
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(id, requestedEntity, new UpdateResolvingPiContinuation<T>() {
            @Override
            public T update(T existingEntity, T requestedEntity) {
                updateCacheWithEntityReadFromDhtAndReturnFreshest(id, existingEntity);
                return updateResolvingContinuation.update(existingEntity, requestedEntity);
            }

            @Override
            public void handleException(Exception e) {
                updateResolvingContinuation.receiveException(e);
            }

            @Override
            public void handleResult(T result) {
                updateResolvingContinuation.receiveResult(result);
            }
        });
    }

    @SuppressWarnings(UNCHECKED)
    private <T extends PiEntity> T updateCacheWithEntityReadFromDhtAndReturnFreshest(PId id, T entityReadFromDht) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("getFreshestEntityAndUpdateCacheWithEntityReadFromDht(%s, %s)", id.toStringFull(), entityReadFromDht));

        synchronized (cache) {
            Element e = cache.get(id);
            if (entityReadFromDht != null && e != null) {
                T existingEntity = (T) e.getObjectValue();
                if (existingEntity != null && existingEntity.getVersion() > entityReadFromDht.getVersion()) {
                    LOG.warn(String.format("Cache already contains a later version (%s vs %s) for id %s, using that", existingEntity.getVersion(), entityReadFromDht.getVersion(), id.toStringFull()));
                    return existingEntity;
                } else {
                    LOG.debug(String.format("Updating cache for id %s with result from dht", id.toStringFull()));
                    cache.put(new Element(id, entityReadFromDht));
                    return entityReadFromDht;
                }
            } else if (entityReadFromDht != null) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("Updating cache for id %s with %s", id.toStringFull(), entityReadFromDht));
                cache.put(new Element(id, entityReadFromDht));
                return entityReadFromDht;
            } else if (e != null) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("DHT returned null for id %s, so returning the cached value %s", id.toStringFull(), e.getObjectValue()));
                return (T) e.getObjectValue();
            } else {
                LOG.debug(String.format("No record in cache or dht for id %s, so returning null", id.toStringFull()));
                return null;
            }
        }
    }
}

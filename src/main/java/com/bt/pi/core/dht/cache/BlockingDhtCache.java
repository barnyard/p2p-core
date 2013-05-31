package com.bt.pi.core.dht.cache;

import javax.annotation.Resource;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

/**
 * Blocking implementation of DhtCache used to add caching functionality to DHT reads and writes.
 * 
 * Note: BlockingDhtCache and {@link DhtCache} may use different cache store depending on applicationContext
 * configuration. By default only 100 items will be kept in the cache at one time. Additionally, items will be cached
 * for 10 minutes by default.
 * 
 */
public class BlockingDhtCache {
    private static final Log LOG = LogFactory.getLog(BlockingDhtCache.class);
    private static final String UNCHECKED = "unchecked";

    private DhtClientFactory dhtClientFactory;
    private Cache cache;

    public BlockingDhtCache() {
        this.cache = null;
        this.dhtClientFactory = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    public void setCache(Cache aCache) {
        this.cache = aCache;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PiEntity> T get(PId id) {
        LOG.debug(String.format("get(%s)", id));
        Element e = this.cache.get(id);
        if (e != null) {
            LOG.debug(String.format("found %s in cache", id.toStringFull()));
            return (T) e.getObjectValue();
        }
        return (T) getReadThrough(id);
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PiEntity> T getReadThrough(PId id) {
        LOG.debug(String.format("getReadThrough(%s)", id));
        BlockingDhtReader blockingReader = this.dhtClientFactory.createBlockingReader();
        PiEntity result = blockingReader.get(id);
        if (null != result)
            this.cache.put(getElement(id, result));
        return (T) result;
    }

    private Element getElement(PId id, PiEntity piEntity) {
        return new Element(id, piEntity);
    }

    public <T extends PiEntity> void update(final PId id, final UpdateResolver<T> updateResolver) {
        LOG.debug(String.format("update(%s, %s)", id, updateResolver));
        BlockingDhtWriter blockingWriter = this.dhtClientFactory.createBlockingWriter();
        blockingWriter.update(id, null, new UpdateResolver<T>() {
            @Override
            public T update(T existingEntity, T requestedEntity) {
                return updateResolver.update(existingEntity, requestedEntity);
            }
        });
        PiEntity valueWritten = blockingWriter.getValueWritten();
        if (null != valueWritten)
            cache.put(getElement(id, valueWritten));
    }

    public boolean writeIfAbsent(final PId id, final PiEntity piEntity) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("writeIfAbsent(%s, %s)", id, piEntity));

        BlockingDhtWriter blockingWriter = this.dhtClientFactory.createBlockingWriter();
        boolean result = blockingWriter.writeIfAbsent(id, piEntity);
        PiEntity valueWritten = blockingWriter.getValueWritten();
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Value %s written to id %s ", piEntity, id.toStringFull()));

        if (null != valueWritten)
            cache.put(getElement(id, valueWritten));
        return result;
    }

    protected Cache getCache() {
        return cache;
    }
}

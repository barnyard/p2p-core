package com.bt.pi.core.application.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategyFactory;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.GenericUpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public abstract class CachingConsumedResourceRegistry extends AbstractConsumedResourceRegistry<PId> {
    private static final Log LOG = LogFactory.getLog(CachingConsumedResourceRegistry.class);
    private static final String UNCHECKED = "unchecked";
    private DhtCache dhtCache;
    private SharedResourceWatchingStrategyFactory sharedResourceWatchingStrategyFactory;

    public CachingConsumedResourceRegistry() {
        dhtCache = null;
        sharedResourceWatchingStrategyFactory = null;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        this.dhtCache = aDhtCache;
    }

    @Resource
    public void setSharedResourceWatchingStrategyFactory(SharedResourceWatchingStrategyFactory aSharedResourceWatchingStrategyFactory) {
        sharedResourceWatchingStrategyFactory = aSharedResourceWatchingStrategyFactory;
    }

    @Override
    protected String getKeyAsString(PId resourceId) {
        return resourceId.toStringFull();
    }

    @SuppressWarnings(UNCHECKED)
    public void registerConsumer(final PId resourceId, final String consumerId, final Class<? extends PiEntity> resourceEntityType, final Continuation<Boolean, Exception> resultContinuation) {
        SharedResourceWatchingStrategy<PId> sharedResourceWatchingStrategy = (SharedResourceWatchingStrategy<PId>) sharedResourceWatchingStrategyFactory.createWatchingStrategy(resourceEntityType);
        super.registerConsumer(resourceId, consumerId, sharedResourceWatchingStrategy, resultContinuation);
    }

    @Override
    protected void initializeResourceAndCreateState(final PId resourceId, final String consumerId, final GenericContinuation<ConsumedResourceState<PId>> resultContinuation) {
        dhtCache.getReadThrough(resourceId, new GenericContinuation<PiEntity>() {
            @Override
            public void handleResult(PiEntity newEntity) {
                LOG.debug(String.format("handleResult(%s)", newEntity));
                ConsumedResourceState<PId> consumedResourceState = new CachedConsumedResourceState<PId>(resourceId, newEntity);
                resultContinuation.receiveResult(consumedResourceState);
            }

            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format("Error whilst registering entity %s: %s. Delegating handling to passed continuation. Exception %s \n %s", resourceId, resultContinuation, e, Arrays.toString(e.getStackTrace())));
                resultContinuation.receiveException(e);
            }
        });
    }

    public <T extends PiEntity> void refresh(final PId resourceId, final Continuation<T, Exception> refreshCompletedContinuation) {
        LOG.debug(String.format("refresh(%s, %s)", resourceId, refreshCompletedContinuation));
        if (!getResourceMap().containsKey(resourceId)) {
            LOG.debug(String.format("Resource %s is not being managed by %s", resourceId, getClass().getName()));
            return;
        }

        dhtCache.getReadThrough(resourceId, new GenericContinuation<T>() {
            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format("Error refreshing entity %s: %s. Delegating handling to passed continuation", resourceId, refreshCompletedContinuation));
                refreshCompletedContinuation.receiveException(e);
            }

            @Override
            public void handleResult(T newEntity) {
                if (newEntity != null) {
                    LOG.debug(String.format("Refreshing entity for cached resource %s with %s", resourceId, newEntity));
                    synchronized (getLock()) {
                        CachedConsumedResourceState<PId> resourceState = (CachedConsumedResourceState<PId>) getResourceMap().get(resourceId);
                        if (resourceState == null)
                            throw new NullPointerException(String.format("Resource state for %s was unexpectedly null in resource map", resourceId));
                        resourceState.setEntity(newEntity);
                    }
                    refreshCompletedContinuation.receiveResult(newEntity);
                } else {
                    LOG.debug(String.format("Null entity %s got from DHT", resourceId));
                    refreshCompletedContinuation.receiveResult(null);
                }
            }
        });
    }

    public <T extends PiEntity> void update(PId resourceId, UpdateResolvingContinuation<T, Exception> updateResolvingContinuation) {
        update(resourceId, null, updateResolvingContinuation);
    }

    public <T extends PiEntity> void update(final PId resourceId, final T requestedEntity, final UpdateResolvingContinuation<T, Exception> updateResolvingContinuation) {
        final CachedConsumedResourceState<PId> resourceState = (CachedConsumedResourceState<PId>) getResourceMap().get(resourceId);
        if (resourceState == null) {
            LOG.warn(String.format("Asked to update non-managed resource %s", resourceId));
            return;
        }

        LOG.debug(String.format("About to update %s", resourceState));
        dhtCache.update(resourceId, requestedEntity, new GenericUpdateResolvingContinuation<T>() {
            @Override
            public T update(T existingEntity, T requestedEntity) {
                LOG.debug(String.format("Delegating update..."));
                return updateResolvingContinuation.update(existingEntity, requestedEntity);
            }

            @Override
            public void handleException(Exception e) {
                LOG.debug(String.format("Error updating entity %s: %s. Delegating handling to passed continuation", resourceId, updateResolvingContinuation));
                updateResolvingContinuation.receiveException(e);
            }

            @Override
            public void handleResult(T newEntity) {
                if (newEntity != null) {
                    LOG.debug(String.format("Caching updated resource %s with %s", resourceId, newEntity));
                    synchronized (getLock()) {
                        resourceState.setEntity(newEntity);
                    }
                } else {
                    LOG.debug(String.format("Not updating cached entity %s from DHT as it was not written", resourceId));
                }
                updateResolvingContinuation.receiveResult(newEntity);
            }
        });
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PiEntity> T getCachedEntity(PId resourceId) {
        LOG.debug(String.format("getCachedEntity(%s)", resourceId));
        CachedConsumedResourceState<PId> resource = (CachedConsumedResourceState<PId>) getResourceMap().get(resourceId);
        if (resource == null) {
            LOG.debug(String.format("Resource %s is not a managed resource", resourceId.toStringFull()));
            return null;
        }

        T res = null;
        synchronized (getLock()) {
            res = (T) resource.getEntity();
        }
        LOG.debug(String.format("Got cached entity %s", res));
        return res;
    }

    @SuppressWarnings(UNCHECKED)
    public <T> List<T> getByType(Class<T> clazz) {
        LOG.debug(String.format("getByType(Class - %s)", clazz));
        List<T> res = new ArrayList<T>();
        synchronized (getLock()) {
            for (Entry<PId, ConsumedResourceState<PId>> entry : getResourceMap().entrySet()) {
                CachedConsumedResourceState<PId> resource = (CachedConsumedResourceState<PId>) entry.getValue();
                Object cachedEntity = resource.getEntity();
                if (cachedEntity != null && cachedEntity.getClass().equals(clazz)) {
                    res.add((T) cachedEntity);
                }
            }
        }
        LOG.debug("Returning list: " + res);
        return res;
    }

    @SuppressWarnings(UNCHECKED)
    public <T extends PiEntity> void clearAll(Class<T> clazz) {
        LOG.debug(String.format("clearAll(Class - %s)", clazz));
        synchronized (getLock()) {
            Map<PId, HashSet<String>> consumersToRemove = new ConcurrentHashMap<PId, HashSet<String>>();
            for (Entry<PId, ConsumedResourceState<PId>> entry : getResourceMap().entrySet()) {
                CachedConsumedResourceState<PId> resourceState = (CachedConsumedResourceState<PId>) entry.getValue();
                T cachedEntity = (T) resourceState.getEntity();
                if (cachedEntity != null && cachedEntity.getClass().equals(clazz)) {
                    consumersToRemove.put(resourceState.getId(), new HashSet<String>());
                    for (String consumerId : resourceState.getConsumerSet()) {
                        consumersToRemove.get(resourceState.getId()).add(consumerId);
                    }
                }
            }

            for (Entry<PId, HashSet<String>> entryToRemove : consumersToRemove.entrySet()) {
                deregisterConsumers(entryToRemove.getKey(), entryToRemove.getValue());
            }
        }
    }
}

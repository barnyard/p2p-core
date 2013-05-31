package com.bt.pi.core.application.resource;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.GenericContinuation;

public abstract class AbstractConsumedResourceRegistry<ResourceIdType> {
    private static final String S_RESOURCE_REFRESH_RUNNER = "%s-resource-refresh-runner";
    private static final String S_CONSUMER_WATCHER = "%s-%s-consumer-watcher";
    private static final String RESOURCE_S_DID_NOT_EXIST_AND_SO_IS_BEING_CREATED = "Resource %s did not exist, and so is being created";

    private static final Log LOG = LogFactory.getLog(AbstractConsumedResourceRegistry.class);
    private ConcurrentHashMap<ResourceIdType, ConsumedResourceState<ResourceIdType>> resourceMap;
    private Object lock;
    private WatcherService watcherService;

    public AbstractConsumedResourceRegistry() {
        resourceMap = new ConcurrentHashMap<ResourceIdType, ConsumedResourceState<ResourceIdType>>();
        lock = new Object();
        watcherService = null;
    }

    @Resource
    public void setWatcherService(WatcherService aWatcherService) {
        watcherService = aWatcherService;
    }

    protected abstract String getKeyAsString(ResourceIdType resourceId);

    protected ConcurrentHashMap<ResourceIdType, ConsumedResourceState<ResourceIdType>> getResourceMap() {
        return resourceMap;
    }

    protected Object getLock() {
        return lock;
    }

    public void registerConsumer(final ResourceIdType resourceId, final String consumerId, final Continuation<Boolean, Exception> resultContinuation) {
        registerConsumer(resourceId, consumerId, createNoOpResourceWatchingStrategy(), resultContinuation);
    }

    protected void registerConsumer(final ResourceIdType resourceId, final String consumerId, final SharedResourceWatchingStrategy<ResourceIdType> watchingStrategy, final Continuation<Boolean, Exception> resultContinuation) {
        LOG.info(String.format("Registering consumer %s for resource %s with watching strategy %s", consumerId, getKeyAsString(resourceId), watchingStrategy.getClass().getName()));
        final Runnable consumerWatcher = watchingStrategy.getConsumerWatcher(resourceId, consumerId);
        synchronized (lock) {
            ConsumedResourceState<ResourceIdType> existingResource = resourceMap.get(resourceId);
            if (existingResource != null) {
                boolean res = existingResource.registerConsumer(consumerId);
                if (consumerWatcher != null) {
                    addConsumerWatcherTask(resourceId, consumerId, consumerWatcher, watchingStrategy.getInitialConsumerWatcherIntervalMillis(), watchingStrategy.getRepeatingConsumerWatcherIntervalMillis());
                }

                LOG.debug(String.format("Register of consumer %s for resource %s returning %s", consumerId, resourceId, res));
                resultContinuation.receiveResult(res);
                return;
            }

            LOG.info(String.format(RESOURCE_S_DID_NOT_EXIST_AND_SO_IS_BEING_CREATED, resourceId));
            initializeResourceAndCreateState(resourceId, consumerId, new GenericContinuation<ConsumedResourceState<ResourceIdType>>() {
                @Override
                public void handleException(Exception e) {
                    resultContinuation.receiveException(e);
                }

                @Override
                public void handleResult(ConsumedResourceState<ResourceIdType> newlyCreatedState) {
                    ConsumedResourceState<ResourceIdType> consumedResourceState = newlyCreatedState;
                    synchronized (lock) {
                        ConsumedResourceState<ResourceIdType> existingConsumedResourceState = resourceMap.putIfAbsent(resourceId, consumedResourceState);
                        if (existingConsumedResourceState != null) {
                            LOG.debug(String.format("Resource creation of %s for %s gave us back an existing resource", resourceId, consumerId));
                            consumedResourceState = existingConsumedResourceState;
                        } else {
                            LOG.debug(String.format("Added resource %s", resourceId));
                        }
                    }

                    boolean res = consumedResourceState.registerConsumer(consumerId);
                    LOG.debug(String.format("Register result: %s", res));
                    resultContinuation.receiveResult(res);

                    Runnable resourceRefreshRunner = watchingStrategy.getSharedResourceRefreshRunner(resourceId);
                    if (res && resourceRefreshRunner != null) {
                        addResourceWatcherTask(resourceId, resourceRefreshRunner, watchingStrategy.getInitialResourceRefreshIntervalMillis(), watchingStrategy.getRepeatingResourceRefreshIntervalMillis());
                    }
                    if (consumerWatcher != null) {
                        addConsumerWatcherTask(resourceId, consumerId, consumerWatcher, watchingStrategy.getInitialConsumerWatcherIntervalMillis(), watchingStrategy.getRepeatingConsumerWatcherIntervalMillis());
                    }
                }
            });
        }
    }

    protected SharedResourceWatchingStrategy<ResourceIdType> createNoOpResourceWatchingStrategy() {
        return new NoOpSharedResourceWatchingStrategy<ResourceIdType>();
    }

    protected void initializeResourceAndCreateState(ResourceIdType resourceId, String consumerId, GenericContinuation<ConsumedResourceState<ResourceIdType>> resultContinuation) {
        LOG.debug(String.format("createResourceState(%s, %s)", resourceId, consumerId));
        ConsumedResourceState<ResourceIdType> consumedResourceState = new ConsumedResourceState<ResourceIdType>(resourceId);
        resultContinuation.receiveResult(consumedResourceState);
    }

    public boolean deregisterConsumer(ResourceIdType resourceId, String consumerId) {
        LOG.info(String.format("Deregistering consumer %s for resource %s", consumerId, resourceId));
        boolean res = false;
        synchronized (lock) {
            ConsumedResourceState<ResourceIdType> resource = resourceMap.get(resourceId);
            if (resource != null) {
                res = resource.deregisterConsumer(consumerId);
                if (res) {
                    LOG.info(String.format("Resource %s has no more consumers and is being removed", resourceId));
                    removeResourceWatcherTask(resourceId);
                    resourceMap.remove(resourceId);
                }
                removeConsumerWatcherTask(resourceId, consumerId);
            } else {
                LOG.info(String.format("Resource %s is an unknown resource", resourceId));
            }
        }
        LOG.debug(String.format("Deregister of consumer %s for resource %s returning %s", consumerId, resourceId, res));
        return res;
    }

    public Set<String> getAllConsumers(ResourceIdType resourceId) {
        LOG.debug(String.format("getAllConsumers(%s)", resourceId));
        Set<String> res = new HashSet<String>();
        synchronized (lock) {
            ConsumedResourceState<ResourceIdType> resource = resourceMap.get(resourceId);
            if (resource != null) {
                res.addAll(resource.getConsumerSet());
            }
        }
        return res;
    }

    public void clearResource(ResourceIdType resourceId) {
        LOG.debug(String.format("clearResource(%s)", resourceId));
        synchronized (lock) {
            ConsumedResourceState<ResourceIdType> resource = resourceMap.get(resourceId);
            if (resource == null) {
                LOG.info(String.format("No resource found when attempting to clear it: %s", resourceId));
                return;
            }
            Set<String> consumers = resource.getConsumerSet();
            deregisterConsumers(resourceId, consumers);
        }
    }

    protected void deregisterConsumers(final ResourceIdType resourceId, final Set<String> consumers) {
        if (consumers.isEmpty())
            return;

        int numRemoved = 0;
        Set<String> consumersCopy = new TreeSet<String>();
        consumersCopy.addAll(consumers);

        for (String consumerId : consumersCopy) {
            deregisterConsumer(resourceId, consumerId);
            numRemoved++;
        }
        LOG.debug(String.format("Removed %d consumer(s) of resource %s", numRemoved, resourceId));
    }

    protected void addResourceWatcherTask(ResourceIdType resourceId, Runnable refreshRunner, long initialInterval, long repeatingInterval) {
        String taskName = String.format(S_RESOURCE_REFRESH_RUNNER, getKeyAsString(resourceId));
        LOG.debug(String.format("Adding resource watcher for %s as task %s", resourceId, taskName));
        watcherService.replaceTask(taskName, refreshRunner, initialInterval, repeatingInterval);
    }

    protected void addConsumerWatcherTask(ResourceIdType resourceId, String consumerId, Runnable consumerWatcher, long initialInterval, long repeatingInterval) {
        String taskName = String.format(S_CONSUMER_WATCHER, getKeyAsString(resourceId), consumerId);
        LOG.debug(String.format("Adding consumer watcher for %s as task %s", consumerId, taskName));
        watcherService.replaceTask(taskName, consumerWatcher, initialInterval, repeatingInterval);
    }

    protected void removeResourceWatcherTask(ResourceIdType resourceId) {
        String taskName = String.format(S_RESOURCE_REFRESH_RUNNER, getKeyAsString(resourceId));
        LOG.debug(String.format("Removing resource watcher for %s", resourceId));
        watcherService.removeTask(taskName);
    }

    protected void removeConsumerWatcherTask(ResourceIdType resourceId, String consumerId) {
        String taskName = String.format(S_CONSUMER_WATCHER, getKeyAsString(resourceId), consumerId);
        LOG.debug(String.format("Removing consumer watcher for %s", consumerId));
        watcherService.removeTask(taskName);
    }
}

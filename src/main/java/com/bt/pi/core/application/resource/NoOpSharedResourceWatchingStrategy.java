package com.bt.pi.core.application.resource;

import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;


public class NoOpSharedResourceWatchingStrategy<ResourceIdType> implements SharedResourceWatchingStrategy<ResourceIdType> {
    public NoOpSharedResourceWatchingStrategy() {
    }

    @Override
    public Runnable getConsumerWatcher(ResourceIdType resourceId, String consumerId) {
        return null;
    }

    @Override
    public long getInitialConsumerWatcherIntervalMillis() {
        return 0;
    }

    @Override
    public long getInitialResourceRefreshIntervalMillis() {
        return 0;
    }

    @Override
    public long getRepeatingConsumerWatcherIntervalMillis() {
        return 0;
    }

    @Override
    public long getRepeatingResourceRefreshIntervalMillis() {
        return 0;
    }

    @Override
    public Runnable getSharedResourceRefreshRunner(ResourceIdType resourceId) {
        return null;
    }

    @Override
    public void setInitialConsumerWatcherIntervalMillis(long val) {
    }

    @Override
    public void setInitialResourceRefreshIntervalMillis(long val) {
    }

    @Override
    public void setRepeatingConsumerWatcherIntervalMillis(long val) {
    }

    @Override
    public void setRepeatingResourceRefreshIntervalMillis(long val) {
    }

}

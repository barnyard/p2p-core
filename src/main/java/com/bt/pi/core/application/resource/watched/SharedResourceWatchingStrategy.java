package com.bt.pi.core.application.resource.watched;

public interface SharedResourceWatchingStrategy<ResourceIdType> {
    Runnable getSharedResourceRefreshRunner(ResourceIdType resourceId);

    Runnable getConsumerWatcher(ResourceIdType resourceId, String consumerId);

    long getInitialResourceRefreshIntervalMillis();

    long getRepeatingResourceRefreshIntervalMillis();

    long getInitialConsumerWatcherIntervalMillis();

    long getRepeatingConsumerWatcherIntervalMillis();

    void setInitialResourceRefreshIntervalMillis(long val);

    void setRepeatingResourceRefreshIntervalMillis(long val);

    void setInitialConsumerWatcherIntervalMillis(long val);

    void setRepeatingConsumerWatcherIntervalMillis(long val);
}

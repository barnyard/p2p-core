package com.bt.pi.core.application.resource;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;
import com.bt.pi.core.conf.Property;

public abstract class AbstractResourceWatchingStrategy<ResourceIdType, ResourceType> implements SharedResourceWatchingStrategy<ResourceIdType> {
    private static final Log LOG = LogFactory.getLog(AbstractResourceWatchingStrategy.class);
    private static final String SHARED_RESOURCE_REFRESH_INITIAL_WAIT_TIME_MILIS = "shared.resource.refresh.initial.wait.time.milis";
    private static final String SHARED_RESOURCE_REFRESH_INTERVAL_MILIS = "shared.resource.refresh.interval.milis";
    private static final String SHARED_RESOURCE_CONSUMER_INITIAL_WAIT_TIME_MILIS = "shared.resource.consumer.initial.wait.time.milis";
    private static final String SHARED_RESOURCE_CONSUMER_INTERVAL_MILIS = "shared.resource.consumer.interval.milis";
    private static final String DEFAULT_RESOURCE_REFRESH_INITIAL_WAIT_TIME_MILIS = "300000";
    private static final String DEFAULT_RESOURCE_REFRESH_INTERVAL_MILIS = "600000";
    private static final String DEFAULT_CONSUMER_INITIAL_WAIT_TIME_MILIS = "240000";
    private static final String DEFAULT_RESOURCE_CONSUMER_INTERVAL_MILIS = "660000";
    private CachingConsumedResourceRegistry cachingConsumedResourceRegistry;
    private long initialConsumerWatcherIntervalMillis;
    private long initialResourceRefreshIntervalMillis;
    private long repeatingConsumerWatcherIntervalMillis;
    private long repeatingResourceRefreshIntervalMillis;
    private LeasedResourceAllocationRecordHeartbeater leasedResourceAllocationRecordHeartbeater;

    public AbstractResourceWatchingStrategy() {
        this.cachingConsumedResourceRegistry = null;
        this.initialConsumerWatcherIntervalMillis = Long.parseLong(DEFAULT_CONSUMER_INITIAL_WAIT_TIME_MILIS);
        this.initialResourceRefreshIntervalMillis = Long.parseLong(DEFAULT_RESOURCE_REFRESH_INITIAL_WAIT_TIME_MILIS);
        this.repeatingConsumerWatcherIntervalMillis = Long.parseLong(DEFAULT_RESOURCE_CONSUMER_INTERVAL_MILIS);
        this.repeatingResourceRefreshIntervalMillis = Long.parseLong(DEFAULT_RESOURCE_REFRESH_INTERVAL_MILIS);
    }

    @Override
    public long getRepeatingConsumerWatcherIntervalMillis() {
        return this.repeatingConsumerWatcherIntervalMillis;
    }

    @Property(key = SHARED_RESOURCE_CONSUMER_INTERVAL_MILIS, defaultValue = DEFAULT_RESOURCE_CONSUMER_INTERVAL_MILIS)
    public void setRepeatingConsumerWatcherIntervalMillis(long value) {
        this.repeatingConsumerWatcherIntervalMillis = value;
    }

    @Override
    public long getRepeatingResourceRefreshIntervalMillis() {
        return this.repeatingResourceRefreshIntervalMillis;
    }

    @Property(key = SHARED_RESOURCE_REFRESH_INTERVAL_MILIS, defaultValue = DEFAULT_RESOURCE_REFRESH_INTERVAL_MILIS)
    public void setRepeatingResourceRefreshIntervalMillis(long value) {
        this.repeatingResourceRefreshIntervalMillis = value;
    }

    @Override
    public long getInitialResourceRefreshIntervalMillis() {
        return this.initialResourceRefreshIntervalMillis;
    }

    @Property(key = SHARED_RESOURCE_REFRESH_INITIAL_WAIT_TIME_MILIS, defaultValue = DEFAULT_RESOURCE_REFRESH_INITIAL_WAIT_TIME_MILIS)
    public void setInitialResourceRefreshIntervalMillis(long value) {
        this.initialResourceRefreshIntervalMillis = value;
    }

    @Property(key = SHARED_RESOURCE_CONSUMER_INITIAL_WAIT_TIME_MILIS, defaultValue = DEFAULT_CONSUMER_INITIAL_WAIT_TIME_MILIS)
    public void setInitialConsumerWatcherIntervalMillis(long value) {
        this.initialConsumerWatcherIntervalMillis = value;
    }

    @Override
    public long getInitialConsumerWatcherIntervalMillis() {
        return this.initialConsumerWatcherIntervalMillis;
    }

    @Resource
    public void setCachingConsumedResourceRegistry(CachingConsumedResourceRegistry aCachingConsumedResourceRegistry) {
        this.cachingConsumedResourceRegistry = aCachingConsumedResourceRegistry;
    }

    @Resource
    public void setLeasedResourceAllocationRecordHeartbeater(LeasedResourceAllocationRecordHeartbeater aLeasedResourceAllocationRecordHeartbeater) {
        this.leasedResourceAllocationRecordHeartbeater = aLeasedResourceAllocationRecordHeartbeater;
    }

    protected LeasedResourceAllocationRecordHeartbeater getLeasedResourceAllocationRecordHeartbeater() {
        return leasedResourceAllocationRecordHeartbeater;
    }

    protected CachingConsumedResourceRegistry getCachingConsumedResourceRegistry() {
        if (cachingConsumedResourceRegistry == null) {
            LOG.warn("null cachingConsumedRegistry");
            throw new NullPointerException("Resource registry has not been set! If this is desired behavour, override this method.");
        }
        return cachingConsumedResourceRegistry;
    }

    public abstract Runnable getSharedResourceRefreshRunner(ResourceIdType resourceId);

    @Override
    public Runnable getConsumerWatcher(ResourceIdType resourceId, String consumerId) {
        return null;
    };
}

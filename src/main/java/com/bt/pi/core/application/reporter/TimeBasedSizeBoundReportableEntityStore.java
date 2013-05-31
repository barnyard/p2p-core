package com.bt.pi.core.application.reporter;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class TimeBasedSizeBoundReportableEntityStore<T extends TimeBasedReportableEntity<?>> extends EhCacheReportableEntityStoreBase<T> implements ReportableEntityStore<T> {
    public TimeBasedSizeBoundReportableEntityStore(String name, int timeToIdleInSeconds, int timeToLiveInSeconds, int size) {
        super(name, timeToIdleInSeconds, timeToLiveInSeconds);

        getEhCacheFactoryBean().setMemoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.FIFO);
        getEhCacheFactoryBean().setMaxElementsInMemory(size);
        initEhCache();
    }
}

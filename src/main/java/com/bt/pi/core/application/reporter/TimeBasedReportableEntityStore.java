package com.bt.pi.core.application.reporter;

public class TimeBasedReportableEntityStore<T extends TimeBasedReportableEntity<?>> extends EhCacheReportableEntityStoreBase<T> implements ReportableEntityStore<T> {
    public TimeBasedReportableEntityStore(String name, int timeToIdleInSeconds, int timeToLiveInSeconds) {
        super(name, timeToIdleInSeconds, timeToLiveInSeconds);

        initEhCache();
    }
}

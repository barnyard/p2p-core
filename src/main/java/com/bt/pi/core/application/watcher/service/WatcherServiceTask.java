package com.bt.pi.core.application.watcher.service;

import java.util.concurrent.Future;

class WatcherServiceTask {
    private String name;
    private Runnable runnable;
    private Future<?> future;
    private long initialIntervalMillis;
    private long repeatingIntervalMillis;

    public WatcherServiceTask(String aName, Runnable aRunnable, long aInitialIntervalMillis, long aRepeatingIntervalMillis) {
        super();
        this.name = aName;
        this.runnable = aRunnable;
        this.initialIntervalMillis = aInitialIntervalMillis;
        this.repeatingIntervalMillis = aRepeatingIntervalMillis;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public Future<?> getFuture() {
        return future;
    }

    public long getInitialIntervalMillis() {
        return initialIntervalMillis;
    }

    public long getRepeatingIntervalMillis() {
        return repeatingIntervalMillis;
    }

    public void setFuture(Future<?> aFuture) {
        this.future = aFuture;
    }

    public String getName() {
        return name;
    }
}

package com.bt.pi.core.application.watcher.task;

import javax.annotation.Resource;

import com.bt.pi.core.application.watcher.WatcherApplication;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;

public abstract class TaskProcessingQueueWatcherInitiatorBase {
    private static final String DEFAULT_NUMBER_OF_ITEMS_TO_PROCESS = "5";
    private String watcherName;
    private PiLocation queueLocation;
    private int staleQueueItemMillis;
    private long initialQueueWatcherIntervalMillis;
    private long repeatingQueueWatcherIntervalMillis;

    private TaskProcessingQueueContinuation taskProcessingQueueContinuation;
    private TaskProcessingQueueRetriesExhaustedContinuation taskProcessingQueueRetriesExhaustedContinuation;
    private WatcherService watcherService;
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;
    private WatcherApplication watcherApplication;
    private int numberOfItemsToProcess = Integer.parseInt(DEFAULT_NUMBER_OF_ITEMS_TO_PROCESS);

    public TaskProcessingQueueWatcherInitiatorBase(String aWatcherName, PiLocation aQueueLocation) {
        watcherName = aWatcherName;
        queueLocation = aQueueLocation;

        taskProcessingQueueContinuation = null;
        taskProcessingQueueRetriesExhaustedContinuation = null;
        watcherService = null;
        dhtClientFactory = null;
        koalaIdFactory = null;
        watcherApplication = null;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setWatcherService(WatcherService aWatcherService) {
        watcherService = aWatcherService;
    }

    @Resource
    public void setWatcherApplication(WatcherApplication aWatcherApplication) {
        watcherApplication = aWatcherApplication;
    }

    @Property(key = "task.processing.number.items", defaultValue = DEFAULT_NUMBER_OF_ITEMS_TO_PROCESS)
    public void setNumberOfItemsToProcess(int theNumberOfItemsToProcess) {
        this.numberOfItemsToProcess = theNumberOfItemsToProcess;
    }

    public KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    public DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    public WatcherService getWatcherService() {
        return watcherService;
    }

    protected void setTaskProcessingQueueContinuation(TaskProcessingQueueContinuation aTaskProcessingQueueContinuation) {
        taskProcessingQueueContinuation = aTaskProcessingQueueContinuation;
    }

    protected void setTaskProcessingQueueRetriesExhaustedContinuation(TaskProcessingQueueRetriesExhaustedContinuation aTaskProcessingQueueRetriesExhaustedContinuation) {
        taskProcessingQueueRetriesExhaustedContinuation = aTaskProcessingQueueRetriesExhaustedContinuation;
    }

    public void setStaleQueueItemMillis(int value) {
        staleQueueItemMillis = value;
    }

    public int getStaleQueueItemMillis() {
        return staleQueueItemMillis;
    }

    public void setInitialQueueWatcherIntervalMillis(long value) {
        initialQueueWatcherIntervalMillis = value;
    }

    public long getInitialQueueWatcherIntervalMillis() {
        return initialQueueWatcherIntervalMillis;
    }

    public void setRepeatingQueueWatcherIntervalMillis(long value) {
        repeatingQueueWatcherIntervalMillis = value;
    }

    public long getRepeatingQueueWatcherIntervalMillis() {
        return repeatingQueueWatcherIntervalMillis;
    }

    public void createTaskProcessingQueueWatcher(String nodeId) {
        TaskProcessingQueueWatcher taskProcessingQueueWatcher = new TaskProcessingQueueWatcher(queueLocation, koalaIdFactory, dhtClientFactory, staleQueueItemMillis, numberOfItemsToProcess, taskProcessingQueueContinuation,
                taskProcessingQueueRetriesExhaustedContinuation, watcherApplication);
        watcherService.replaceTask(watcherName, taskProcessingQueueWatcher, initialQueueWatcherIntervalMillis, repeatingQueueWatcherIntervalMillis);
    }

    public void removeTaskProcessingQueueWatcher() {
        watcherService.removeTask(watcherName);
    }
}

package com.bt.pi.core.application.watcher.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;

@Component
public class WatcherService {
    private static final String DEFAULT_RANDOM_DEVIATION = "0.2";
    private static final long CLOSING_CHECK_INTERVAL_MILLIS = 250;
    private static final Log LOG = LogFactory.getLog(WatcherService.class);
    private SortedMap<Long, List<WatcherServiceTask>> taskQueue;
    private Map<String, Long> queueIndexByTaskName;
    private ScheduledExecutorService scheduledExecutorService;
    private volatile CountDownLatch closingLatch;
    private Random random;
    private float randomDeviation;

    public WatcherService() {
        taskQueue = new TreeMap<Long, List<WatcherServiceTask>>();
        queueIndexByTaskName = new HashMap<String, Long>();
        scheduledExecutorService = null;
        closingLatch = null;
        random = new Random();
        randomDeviation = Float.parseFloat(DEFAULT_RANDOM_DEVIATION);
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Property(key = "watcher.service.random.deviation", defaultValue = DEFAULT_RANDOM_DEVIATION)
    public synchronized void setRandomInterval(float aRandomDeviationPercent) {
        randomDeviation = aRandomDeviationPercent;
    }

    protected SortedMap<Long, List<WatcherServiceTask>> getTaskQueue() {
        return taskQueue;
    }

    public void addTask(String name, Runnable runnable, long initialIntervalMillis, long repeatingIntervalMillis) {
        LOG.debug(String.format("addTask(%s, %s, %d, %d", name, runnable, initialIntervalMillis, repeatingIntervalMillis));
        WatcherServiceTask watcherTask = new WatcherServiceTask(name, runnable, initialIntervalMillis, repeatingIntervalMillis);
        enqueue(watcherTask, initialIntervalMillis);
    }

    public void replaceTask(String name, Runnable runnable, long initialIntervalMillis, long repeatingIntervalMillis) {
        LOG.debug(String.format("replaceTask(%s, %s, %d, %d", name, runnable, initialIntervalMillis, repeatingIntervalMillis));
        WatcherServiceTask watcherTask = new WatcherServiceTask(name, runnable, initialIntervalMillis, repeatingIntervalMillis);
        synchronized (this) {
            removeTask(name);
            enqueue(watcherTask, initialIntervalMillis);
        }
    }

    public void removeTask(String name) {
        LOG.debug(String.format("removeTasks(%s)", name));
        synchronized (this) {
            Long removalTaskTime = queueIndexByTaskName.remove(name);
            if (removalTaskTime == null) {
                LOG.debug(String.format("Task %s won't be removed as it does not exist", name));
                return;
            }

            LOG.debug(String.format("Removing task %s", name));
            List<WatcherServiceTask> tasks = getTaskQueue().get(removalTaskTime);
            if (tasks == null) {
                LOG.warn(String.format("Did not find any tasks for time %d when removing task %s!", removalTaskTime, name));
                return;
            }

            List<WatcherServiceTask> tasksToRemove = new ArrayList<WatcherServiceTask>();
            for (WatcherServiceTask task : tasks) {
                if (task.getName().equals(name)) {
                    tasksToRemove.add(task);
                }
            }

            for (WatcherServiceTask task : tasksToRemove) {
                tasks.remove(task);
                LOG.debug(String.format("Removed task %s", name));
            }
        }
    }

    protected void enqueue(WatcherServiceTask task, long delay) {
        LOG.debug(String.format("enqueue(%s, %d)", task, delay));
        synchronized (this) {
            if (queueIndexByTaskName.containsKey(task.getName())) {
                throw new TaskAlreadyExistsException(String.format("Task %s already exists in watcher service", task.getName()));
            }

            int maxVariation = (int) (delay * randomDeviation);
            long actualDelay = 0;
            if (maxVariation > 0) {
                int randomDelay = random.nextInt(maxVariation);
                actualDelay = random.nextBoolean() ? delay + randomDelay : delay - randomDelay;
            }
            long actualInterval = getNow() + actualDelay;

            List<WatcherServiceTask> list = new ArrayList<WatcherServiceTask>();
            if (!getTaskQueue().containsKey(actualInterval))
                getTaskQueue().put(actualInterval, list);
            else
                list = getTaskQueue().get(actualInterval);
            list.add(task);
            queueIndexByTaskName.put(task.getName(), actualInterval);
        }
    }

    protected long getNow() {
        return System.currentTimeMillis();
    }

    @PostConstruct
    public void start() {
        LOG.info(String.format("Starting %s", this.getClass().getSimpleName()));
        closingLatch = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processQueuedTasks();
                } catch (InterruptedException e) {
                    LOG.debug("Watcher service thread was interrupted! " + e.getMessage(), e);
                }
            }
        }, "Watcher-Service-Thread");
        t.start();
    }

    @PreDestroy
    public void stop() {
        LOG.info(String.format("Stopping %s", this.getClass().getSimpleName()));
        closingLatch.countDown();
    }

    protected void processQueuedTasks() throws InterruptedException {
        while (closingLatch != null && !closingLatch.await(CLOSING_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) {
            if (getTaskQueue().isEmpty())
                continue;

            long first = getTaskQueue().firstKey();
            long now = getNow();
            if (first < now) {
                synchronized (this) {
                    List<WatcherServiceTask> nextTasks = getTaskQueue().remove(first);
                    for (WatcherServiceTask task : nextTasks) {
                        queueIndexByTaskName.remove(task.getName());
                        Future<?> future = task.getFuture();
                        if (future != null && !future.isDone()) {
                            LOG.warn(String.format("Task '%s' not completed %d millis after next run was meant to start! Task won't be run now, but will be scheduled to run again as usual in %d millis", task.getName(), now - first, task
                                    .getRepeatingIntervalMillis()));
                            enqueue(task, task.getRepeatingIntervalMillis());
                            continue;
                        }

                        runTask(task);
                    }
                }
            }
        }
    }

    private void runTask(final WatcherServiceTask task) {
        LOG.debug(String.format("Executing task %s - interval %s", task.getName(), task.getRepeatingIntervalMillis()));
        Future<?> newFuture = scheduledExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    task.getRunnable().run();
                } catch (Throwable t) {
                    LOG.error(String.format("Unhandled exception in watcher task %s: %s", task.getName(), t.getMessage()), t);
                }
            }
        });
        task.setFuture(newFuture);

        LOG.debug(String.format("Scheduling task %s to run in %d millis", task.getName(), task.getRepeatingIntervalMillis()));
        enqueue(task, task.getRepeatingIntervalMillis());
    }
}

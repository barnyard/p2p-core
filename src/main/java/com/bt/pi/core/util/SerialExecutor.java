package com.bt.pi.core.util;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class SerialExecutor implements Executor {
    private static final int FIFTY = 50;
    private static final Log LOG = LogFactory.getLog(SerialExecutor.class);
    private BlockingQueue<SerialExecutorRunnable> tasks;
    private volatile boolean terminateRequested;
    private Set<String> tasksSet;

    public SerialExecutor() {
        this.tasks = new LinkedBlockingQueue<SerialExecutorRunnable>();
        tasksSet = new ConcurrentSkipListSet<String>();
    }

    @Override
    public void execute(final Runnable r) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("execute(%s)", r));
        if (!(r instanceof SerialExecutorRunnable))
            throw new IllegalArgumentException(String.format("Cannot execute runnable of type %s, needs to be a SerialExecutorRunnable", r.getClass()));

        SerialExecutorRunnable runnable = (SerialExecutorRunnable) r;
        if (runnable.getQueueUrl() == null || runnable.getEntityUrl() == null)
            throw new IllegalArgumentException("URL of SerialExecutorRunnable should not be null");

        if (!tasks.offer(runnable))
            LOG.error("task queue not accepting new tasks");

        if (tasks.size() > FIFTY)
            LOG.warn("queue size greater than 50");

        tasksSet.add(getTasksIdentifier(runnable.getQueueUrl(), runnable.getEntityUrl()));
    }

    public boolean isQueuedOrRunning(String queueUrl, String entityUrl) {
        return tasksSet.contains(getTasksIdentifier(queueUrl, entityUrl));
    }

    @PostConstruct
    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!terminateRequested) {
                    String url = null;
                    try {
                        SerialExecutorRunnable toBeRun = tasks.take();
                        url = getTasksIdentifier(toBeRun.getQueueUrl(), toBeRun.getEntityUrl());
                        toBeRun.run();
                    } catch (Throwable t) {
                        LOG.error("error running task", t);
                    } finally {
                        if (url != null)
                            tasksSet.remove(url);
                    }
                }
            }
        }, getClass().getSimpleName()).start();
    }

    private String getTasksIdentifier(String queueUrl, String entityUrl) {
        return String.format("%s:%s", queueUrl, entityUrl);
    }

    @PreDestroy
    public void stop() {
        this.terminateRequested = true;
    }
}
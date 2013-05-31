package com.bt.pi.core.continuation.scattergather;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class ScatterGatherContinuationRunner {
    private static final Log LOG = LogFactory.getLog(ScatterGatherContinuationRunner.class);
    private ScheduledExecutorService scheduledExecutorService;

    public ScatterGatherContinuationRunner() {
        scheduledExecutorService = null;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    public void execute(Collection<ScatterGatherContinuationRunnable> runnables, long timeoutDuration, TimeUnit timeoutUnit) {
        if (runnables != null) {
            CountDownLatch latch = new CountDownLatch(runnables.size());
            for (ScatterGatherContinuationRunnable runnable : runnables) {
                runnable.setLatch(latch);
                scheduledExecutorService.execute(runnable);
            }

            try {
                latch.await(timeoutDuration, timeoutUnit);
            } catch (InterruptedException e) {
                LOG.warn("Exception while waiting for executor task to complete", e);
            }
        }
    }
}

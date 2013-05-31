package com.bt.pi.core.continuation.scattergather;

import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unchecked")
public abstract class ScatterGatherContinuationRunnable implements Runnable {
    private ScatterGatherContinuation scatterGatherContinuation;

    public ScatterGatherContinuationRunnable(ScatterGatherContinuation aScatterGatherContinuation) {
        scatterGatherContinuation = aScatterGatherContinuation;
    }

    public void setLatch(CountDownLatch latch) {
        scatterGatherContinuation.setLatch(latch);
    }
}

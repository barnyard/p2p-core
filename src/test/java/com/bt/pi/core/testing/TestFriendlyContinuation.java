package com.bt.pi.core.testing;

import java.util.concurrent.CountDownLatch;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;

public class TestFriendlyContinuation<T> implements UpdateResolvingContinuation<T, Exception> {
    public CountDownLatch completedLatch = new CountDownLatch(1);
    public CountDownLatch updatedLatch = new CountDownLatch(1);
    public T lastResult;
    public Exception lastException;

    @Override
    public T update(T existingEntity, T requestedEntity) {
        updatedLatch.countDown();
        return existingEntity;
    }

    @Override
    public void receiveException(Exception e) {
        lastException = e;
        completedLatch.countDown();
    }

    @Override
    public void receiveResult(T result) {
        lastResult = result;
        completedLatch.countDown();
    }
};
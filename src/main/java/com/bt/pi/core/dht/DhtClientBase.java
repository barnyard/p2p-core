package com.bt.pi.core.dht;

import java.util.concurrent.Executor;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.past.KoalaDHTStorage;

public abstract class DhtClientBase<T extends PiEntity> extends BlockingContinuationBase<T> {
    private KoalaDHTStorage storage;
    private volatile boolean alreadyUsed;
    private Executor executor;

    public DhtClientBase(Executor anExecutor, KoalaDHTStorage aStorage) {
        this.executor = anExecutor;
        this.storage = aStorage;
        this.alreadyUsed = false;
    }

    protected Executor getExecutor() {
        return this.executor;
    }

    protected KoalaDHTStorage getStorage() {
        return storage;
    }

    protected void checkIfAlreadyUsed() {
        if (alreadyUsed)
            throw new IllegalStateException(String.format("Object %s can only be used for a single operation", getClass().getSimpleName()));
        else
            alreadyUsed = true;
    }

    public void setStorage(KoalaDHTStorage astorage) {
        this.storage = astorage;
    }

    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }
}

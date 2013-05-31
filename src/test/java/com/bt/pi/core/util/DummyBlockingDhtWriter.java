package com.bt.pi.core.util;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;

@Component
public class DummyBlockingDhtWriter implements BlockingDhtWriter {

    public DummyBlockingDhtWriter() {

    }

    @Override
    @Blocking
    public <T extends PiEntity> void put(PId id, T entity) {

    }

    @Override
    @Blocking
    public boolean writeIfAbsent(PId id, PiEntity piEntity) {

        return false;
    }

    @Override
    @Blocking
    public <T extends PiEntity> void update(PId id, T entity, UpdateResolver<T> updateResolver) {

    }

    @Override
    public PiEntity getValueWritten() {

        return null;
    }

    @Override
    public void setStorage(KoalaDHTStorage astorage) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setExecutor(Executor anExecutor) {
        // TODO Auto-generated method stub

    }

}


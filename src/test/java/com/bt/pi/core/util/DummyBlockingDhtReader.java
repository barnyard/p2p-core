package com.bt.pi.core.util;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;

import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.KoalaDHTStorage;

@Component
public class DummyBlockingDhtReader implements BlockingDhtReader {

    public DummyBlockingDhtReader() {

    }

    @Override
    @Blocking
    public PiEntity get(PId id) {

        return null;
    }

    @Override
    @Blocking
    public PiEntity getAny(PId id) {

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


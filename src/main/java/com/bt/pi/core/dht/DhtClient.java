package com.bt.pi.core.dht;

import java.util.concurrent.Executor;

import com.bt.pi.core.past.KoalaDHTStorage;

public interface DhtClient {
    void setStorage(KoalaDHTStorage astorage);

    void setExecutor(Executor anExecutor);
}

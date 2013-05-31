package com.bt.pi.core.application.storage;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.past.content.KoalaGCPastMetadata;

public interface LocalStorageScanningHandler {
    void handle(Id id, KoalaGCPastMetadata metadata);
}

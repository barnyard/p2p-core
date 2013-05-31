package com.bt.pi.core.past.internalcontinuation;

import rice.Continuation;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.messaging.PastMessage;

public interface KoalaPast {
    @SuppressWarnings("unchecked")
    void sendPastRequest(NodeHandle handle, PastMessage message, Continuation command);

    NodeHandle getLocalNodeHandle();

    double getSuccessfulInsertThreshold();
}

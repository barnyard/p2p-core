package com.bt.pi.core.scribe;

public interface SubscribeDataReceivedListener {
    void dataReceived(Object data, int nodeNumber);
}

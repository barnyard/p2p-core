//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import java.util.concurrent.CountDownLatch;

class RequestState<ResponseType> {
    private CountDownLatch countDownLatch;
    private ResponseType response;
    private long creationTimestamp;

    public RequestState() {
        this.countDownLatch = new CountDownLatch(1);
        this.creationTimestamp = System.currentTimeMillis();
        this.response = null;
    }

    public void setResponse(ResponseType aResponse) {
        this.response = aResponse;
    }

    public ResponseType getResponse() {
        return response;
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }
}

//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import java.util.concurrent.TimeUnit;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public abstract class RequestWrapperBase<RequestStateResponseType extends Object, RequestType extends KoalaMessage> {
    protected static final long DEFAULT_REQUEST_TIMEOUT = 10000;
    private long requestTimeoutMillis;

    public RequestWrapperBase() {
        this.requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT;
    }

    public long getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public void setRequestTimeoutMillis(long aRequestTimeoutMillis) {
        this.requestTimeoutMillis = aRequestTimeoutMillis;
    }

    protected boolean awaitResponse(RequestState<RequestStateResponseType> requestState) {
        try {
            return requestState.getCountDownLatch().await(getRequestTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 
     * @param id
     * @param message
     * @return boolean representing if the message was successfully processed.
     */
    public abstract boolean messageReceived(PId id, RequestType message);

}

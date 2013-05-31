package com.bt.pi.core.messaging;

import rice.Continuation;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public interface ContinuationRequestWrapper extends RequestWrapper<KoalaMessage> {
    void sendRequest(PId id, KoalaMessage message, KoalaMessageSender messageSender, Continuation<KoalaMessage, Exception> continuation);
}

package com.bt.pi.core.messaging;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public interface KoalaMessageSender {
    void routeMessage(PId id, KoalaMessage message);
}

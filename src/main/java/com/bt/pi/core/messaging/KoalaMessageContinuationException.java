package com.bt.pi.core.messaging;

import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;

public class KoalaMessageContinuationException extends Exception {
    private static final long serialVersionUID = -4858756928514969808L;

    private final transient EntityResponseCode entityResponseCode;
    private final transient PiEntity payload;

    public KoalaMessageContinuationException(EntityResponseCode anEntityResponseCode, PiEntity aPayload) {
        entityResponseCode = anEntityResponseCode;
        payload = aPayload;
    }

    public EntityResponseCode getEntityResponseCode() {
        return entityResponseCode;
    }

    public PiEntity getPayload() {
        return payload;
    }
}

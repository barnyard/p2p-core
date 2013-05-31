//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.message;

import rice.p2p.commonapi.rawserialization.RawMessage;

import com.bt.pi.core.entity.EntityResponseCode;

public interface KoalaMessage extends RawMessage {
    String getUID();

    String getCorrelationUID();

    String getTransactionUID();

    long getCreatedAt();

    EntityResponseCode getResponseCode();
}

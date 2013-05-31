//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import org.json.JSONObject;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.message.KoalaMessage;

public interface KoalaMessageFactory {
    KoalaMessage createMessage(JSONObject jsonObject);

    PiEntity createPayload(JSONObject json);

    KoalaMessage createMessage(String jsonObject);

    PiEntity createPayload(String json);
}

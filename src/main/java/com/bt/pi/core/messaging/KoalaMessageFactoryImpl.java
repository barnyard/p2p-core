//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.exception.MessageCreationException;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.message.KoalaMessageBase;
import com.bt.pi.core.parser.KoalaJsonParser;

@Component
public class KoalaMessageFactoryImpl implements KoalaMessageFactory {
    private static final String TO_TYPE_MAP = " to typeMap";
    private static final String FAILED_TO_CREATE_MESSAGE_WITH_TYPE_S = "Failed to create message with type %s";
    private static final Log LOG = LogFactory.getLog(KoalaMessageFactoryImpl.class);
    private Map<String, Class<?>> applicationTypeMap;
    private KoalaJsonParser koalaJsonParser;

    public KoalaMessageFactoryImpl() {
        applicationTypeMap = new HashMap<String, Class<?>>();
        koalaJsonParser = null;
    }

    @Resource
    @Autowired
    public void setApplicationMessageTypes(List<KoalaMessageBase> applicationMessages) {
        for (KoalaMessageBase message : applicationMessages) {
            LOG.debug("Adding Message " + message.getKoalaMessageType() + TO_TYPE_MAP);
            applicationTypeMap.put(message.getKoalaMessageType(), message.getClass());
        }
    }

    @Resource
    @Autowired
    public void setApplicationMessagePayloadTypes(List<PiEntity> applicationPayloads) {
        for (PiEntity payload : applicationPayloads) {
            LOG.debug("Adding payload " + payload.getType() + TO_TYPE_MAP);
            applicationTypeMap.put(payload.getType(), payload.getClass());
        }
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser jsonParser) {
        koalaJsonParser = jsonParser;
    }

    @Override
    public PiEntity createPayload(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return createPayload(jsonObject);
        } catch (JSONException e) {
            throw new KoalaException(e);
        }
    }

    @Override
    public PiEntity createPayload(JSONObject jsonObject) {
        String type = jsonObject.optString(PiEntity.TYPE_PARAM);
        return (PiEntity) createObject(jsonObject, type);
    }

    @Override
    public KoalaMessage createMessage(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            return createMessage(jsonObject);
        } catch (JSONException e) {
            throw new KoalaException(e);
        }
    }

    @Override
    public KoalaMessage createMessage(JSONObject jsonObject) {
        String type = jsonObject.optString(KoalaMessageBase.MESSAGE_TYPE_PARAM);
        return (KoalaMessage) createObject(jsonObject, type);
    }

    public Object createObject(JSONObject jsonObject, String type) {
        try {
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("Creating message from %s", jsonObject));
            if (StringUtils.isEmpty(type)) {
                throw new MessageCreationException(String.format("Failed to create message: no message type found in JSON"));
            }

            Class<?> messageClass = applicationTypeMap.get(type);
            LOG.debug("Message class: " + messageClass);
            if (messageClass == null) {
                throw new MessageCreationException(String.format("Unknown message type: %s", type));
            }

            // Constructor<? extends KoalaMessage> constructor =
            // messageClass.getConstructor(JSONObject.class);

            Object res = koalaJsonParser.getObject(jsonObject.toString(), messageClass);
            if (res instanceof KoalaMessageBase)
                ((KoalaMessageBase) res).setKoalaJsonParser(koalaJsonParser);
            return res;
        } catch (IllegalArgumentException e) {
            throw new MessageCreationException(String.format(FAILED_TO_CREATE_MESSAGE_WITH_TYPE_S, type), e);
        } catch (SecurityException e) {
            throw new MessageCreationException(String.format(FAILED_TO_CREATE_MESSAGE_WITH_TYPE_S, type), e);
        }
    }

}

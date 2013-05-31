//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;

@Component
public class KoalaMessageDeserializer implements MessageDeserializer {
    private static final Log LOG = LogFactory.getLog(KoalaMessageDeserializer.class);
    private KoalaMessageFactory koalaMessageFactory;

    public KoalaMessageDeserializer() {
        koalaMessageFactory = null;
    }

    @Resource
    public void setKoalaMessageFactory(KoalaMessageFactory aKoalaMessageFactory) {
        this.koalaMessageFactory = aKoalaMessageFactory;
    }

    public Message deserialize(InputBuffer aInputBuffer, short aType, int aPriority, NodeHandle aNodeHandle) throws IOException {
        try {
            int length = aInputBuffer.readInt();
            byte[] bytes = new byte[length];
            aInputBuffer.read(bytes);
            String content = new String(bytes, "UTF-8");
            LOG.debug(String.format("Received: %.100s", content)); // take care only to print a little bit of the
                                                                   // message

            JSONObject jsonObject = new JSONObject(content);
            return koalaMessageFactory.createMessage(jsonObject);
        } catch (Throwable t) {
            LOG.error("Failed to deserialize message", t);
            throw new RuntimeException(t);
        }
    }

}
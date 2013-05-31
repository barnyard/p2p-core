package com.bt.pi.core.message;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;

public class ApplicationMessage extends KoalaMessageBase {
    public static final String TYPE = "ApplicationMessage";
    private static final long serialVersionUID = 1L;
    private String json;
    private String sourceId;
    private EntityMethod method;
    private EntityResponseCode responseCode;
    private String sourceApplicationName;
    private String destinationApplicationName;

    public ApplicationMessage() {
        super(ApplicationMessage.TYPE);
    }

    public ApplicationMessage(String jsonPayload, String source, EntityMethod aMethod, EntityResponseCode entityResponseCode, String destAppName, String sourceAppName) {
        this();
        json = jsonPayload;
        sourceId = source;
        method = aMethod;
        responseCode = entityResponseCode;
        destinationApplicationName = destAppName;
        sourceApplicationName = sourceAppName;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String jsonPayload) {
        json = jsonPayload;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String id) {
        this.sourceId = id;
    }

    public EntityMethod getMethod() {
        return method;
    }

    public void setMethod(EntityMethod aMethod) {
        this.method = aMethod;
    }

    @Override
    public EntityResponseCode getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(EntityResponseCode aResponseCode) {
        this.responseCode = aResponseCode;
    }

    public void setSourceApplicationName(String sourceAppName) {
        sourceApplicationName = sourceAppName;
    }

    public String getSourceApplicationName() {
        return sourceApplicationName;
    }

    public void setDestinationApplicationName(String destAppName) {
        destinationApplicationName = destAppName;
    }

    public String getDestinationApplicationName() {
        return destinationApplicationName;
    }
}

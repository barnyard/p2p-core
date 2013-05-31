package com.bt.pi.core.scribe.content;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.json.JSONObject;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

import com.bt.pi.core.entity.EntityMethod;

public class KoalaScribeContent implements ScribeContent {
    private static final long serialVersionUID = 1L;
    private static final int THIRTY = 30;
    private String jsonData;
    private NodeHandle sourceNodeHandle;
    private String transactionUID;
    private EntityMethod entityMethod;

    public KoalaScribeContent(NodeHandle sourceHandle, String aTransactionUID, EntityMethod anEntityMethod, JSONObject payload) {
        this(sourceHandle, aTransactionUID, anEntityMethod, payload.toString());
    }

    public KoalaScribeContent(NodeHandle sourceHandle, String aTransactionUID, EntityMethod anEntityMethod, String payload) {
        if (anEntityMethod == null)
            throw new IllegalArgumentException("Null entity method for pub sub content");

        entityMethod = anEntityMethod;
        jsonData = payload;
        sourceNodeHandle = sourceHandle;
        transactionUID = aTransactionUID;
    }

    public NodeHandle getSourceNodeHandle() {
        return sourceNodeHandle;
    }

    public void setSourceNodeHandle(NodeHandle nh) {
        this.sourceNodeHandle = nh;
    }

    public EntityMethod getEntityMethod() {
        return entityMethod;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String getTransactionUID() {
        return transactionUID;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof KoalaScribeContent))
            return false;
        KoalaScribeContent castOther = (KoalaScribeContent) other;
        return new EqualsBuilder().append(jsonData, castOther.jsonData).append(entityMethod, castOther.entityMethod).append(sourceNodeHandle, castOther.sourceNodeHandle).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(jsonData).append(entityMethod).append(sourceNodeHandle).toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append(super.toString()).append(",sourceNodeHandle=" + sourceNodeHandle).append(",entityMethod=" + entityMethod).append(",jsonData=" + StringUtils.abbreviate(jsonData, THIRTY)).toString();
    }
}

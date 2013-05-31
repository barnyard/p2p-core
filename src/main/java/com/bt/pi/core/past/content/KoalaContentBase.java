//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.past.content;

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastException;
import rice.p2p.past.gc.GCPast;
import rice.p2p.past.gc.GCPastContent;
import rice.p2p.past.gc.GCPastContentHandle;
import rice.p2p.past.gc.GCPastMetadata;

public abstract class KoalaContentBase implements GCPastContent {
    private static final Log LOG = LogFactory.getLog(KoalaContentBase.class);
    private static final long serialVersionUID = 1L;
    private Id id;
    private Map<String, String> contentHeaders;

    public KoalaContentBase(Id theId, Map<String, String> headers) {
        id = theId;
        contentHeaders = headers;
    }

    @Override
    public GCPastContentHandle getHandle(GCPast local, long expiration) {
        return new KoalaContentHandleBase(getId(), local.getLocalNodeHandle(), getVersion(), local.getEnvironment());
    }

    @Override
    public PastContentHandle getHandle(Past local) {
        return new KoalaContentHandleBase(getId(), local.getLocalNodeHandle(), getVersion(), local.getEnvironment());
    }

    public Id getId() {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("getId() = %s", id.toStringFull()));
        return id;
    }

    public void setId(Id anId) {
        id = anId;
    }

    public Map<String, String> getContentHeaders() {
        return contentHeaders;
    }

    public void setContentHeaders(Map<String, String> headers) {
        contentHeaders = headers;
    }

    @Override
    public GCPastMetadata getMetadata(long expiration) {
        return new GCPastMetadata(expiration);
    }

    public abstract KoalaContentBase duplicate();

    @Override
    public abstract PastContent checkInsert(Id someId, PastContent existingContent) throws PastException;

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean equals(final Object other) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("%s.isEquals(%s)", this, other));
        if (!(other instanceof KoalaMutableContent))
            return false;
        KoalaContentBase castOther = (KoalaContentBase) other;
        return new EqualsBuilder().append(id, castOther.id).append(contentHeaders, castOther.contentHeaders).isEquals();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("KoalaContentBase [contentHeaders=").append(contentHeaders).append(", id=").append(id).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).toHashCode();
    }

}

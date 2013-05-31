//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.past.content;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;

import com.bt.pi.core.exception.KoalaContentVersionMismatchException;

public class KoalaMutableContent extends KoalaContentBase {
    public static final long IGNORE_VERSION = Long.MIN_VALUE;
    public static final long UNKNOWN_VERSION = -1;

    private static final Log LOG = LogFactory.getLog(KoalaMutableContent.class);
    private static final int THIRTY = 30;
    private static final long serialVersionUID = 1L;

    private String contentBody;

    public KoalaMutableContent(Id theId, String body, Map<String, String> headers) {
        super(theId, headers);
        contentBody = body;
    }

    @Override
    public long getVersion() {
        String contentVersionString = getContentHeaders().get(DhtContentHeader.CONTENT_VERSION);
        if (contentVersionString == null)
            return UNKNOWN_VERSION;
        return Long.parseLong(contentVersionString);
    }

    protected void setVersion(long version) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Setting version to %d", version));
        getContentHeaders().put(DhtContentHeader.CONTENT_VERSION, Long.toString(version));
    }

    @Override
    public PastContent checkInsert(Id someId, PastContent existingContent) throws PastException {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("checkInsert(Id: %s, PastContent %s)", someId, existingContent));
        if (existingContent instanceof KoalaMutableContent) {
            long existingVersion = ((KoalaMutableContent) existingContent).getVersion();
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("checkInsert: version comparison: %d vs. %d", this.getVersion(), existingVersion));
            if (this.getVersion() <= existingVersion && this.getVersion() != KoalaMutableContent.IGNORE_VERSION) {
                throw new KoalaContentVersionMismatchException(String.format("Inserted content: %s version %s is not allowed to update version %s", this, this.getVersion(), existingVersion));
            }
        } else {
            if (LOG.isInfoEnabled())
                LOG.info(String.format("checkInsert: existing content was null or not mutable content, skipping version check"));
        }
        if (LOG.isDebugEnabled())
            LOG.debug("checkInsert returning: " + this);
        return this;
    }

    public KoalaMutableContent duplicate() {
        return new KoalaMutableContent(this.getId(), this.getBody(), new HashMap<String, String>(this.getContentHeaders()));
    }

    public String getBody() {
        return contentBody;
    }

    public void setBody(String body) {
        contentBody = body;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof KoalaMutableContent))
            return false;
        KoalaMutableContent castOther = (KoalaMutableContent) other;
        return new EqualsBuilder().appendSuper(super.equals(other)).append(contentBody, castOther.contentBody).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(contentBody).toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[" + getClass() + "]").append(super.toString()).append(",mutableContent=" + StringUtils.abbreviate(contentBody, THIRTY)).toString();
    }
}

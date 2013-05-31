package com.bt.pi.core.application.activation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonProperty;

public class TimeStampedPair<T extends Comparable<T>> implements Comparable<TimeStampedPair<T>> {
    private static final Log LOG = LogFactory.getLog(TimeStampedPair.class);
    private T item;
    private long timestamp;
    @JsonProperty
    private String hostname;

    public TimeStampedPair() {
    }

    // I couldn't think up a better name than this.
    // rename if you want.
    public TimeStampedPair(T anItem) {
        this(anItem, null);
    }

    public TimeStampedPair(T anItem, Long aTimestamp) {
        if (null == anItem)
            throw new NullPointerException("item cannot be null");
        item = anItem;
        if (aTimestamp != null)
            timestamp = aTimestamp;
        else
            timestamp = generateCurrentTimestamp();
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.error("Unable to retrieve hostname", e);
        }
    }

    public void updateTimeStamp() {
        timestamp = generateCurrentTimestamp();
    }

    protected long generateCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public String getReadableTimestamp() {
        return new Date(timestamp).toString();
    }

    public void setTimeStamp(long aTimestamp) {
        timestamp = aTimestamp;
    }

    public T getObject() {
        return item;
    }

    public void setObject(T object) {
        item = object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof TimeStampedPair))
            return false;
        TimeStampedPair castOther = (TimeStampedPair) other;
        return new EqualsBuilder().append(item, castOther.item).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(item).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(item).append(timestamp).append(hostname).toString();
    }

    public int compareTo(TimeStampedPair<T> o) {
        if (getObject() != null && o != null && o.getObject() != null) {
            return this.getObject().compareTo(o.getObject());
        }
        if (o == null || o.getObject() == null) {
            return -1;
        }
        if (getObject() == null) {
            return 1;
        }
        return 0;
    }
}

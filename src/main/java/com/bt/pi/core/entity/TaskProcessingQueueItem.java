package com.bt.pi.core.entity;

import org.codehaus.jackson.annotate.JsonProperty;

public class TaskProcessingQueueItem {
    private static final int DEFAULT_REMAINING_RETRIES = -1;
    @JsonProperty
    private String url;
    @JsonProperty
    private long lastUpdatedMillis;
    @JsonProperty
    private String ownerNodeId;
    @JsonProperty
    private int remainingRetries;

    public TaskProcessingQueueItem() {
        this.lastUpdatedMillis = System.currentTimeMillis();
        this.remainingRetries = DEFAULT_REMAINING_RETRIES;
    }

    public TaskProcessingQueueItem(String theUrl) {
        this();
        this.url = theUrl;
    }

    public TaskProcessingQueueItem(String theUrl, int aRemainingRetries) {
        this();
        this.url = theUrl;
        this.remainingRetries = aRemainingRetries;
    }

    public TaskProcessingQueueItem(String theUrl, String nodeId) {
        this();
        this.url = theUrl;
        this.ownerNodeId = nodeId;
    }

    public void setUrl(String aUrl) {
        this.url = aUrl;
    }

    public String getUrl() {
        return this.url;
    }

    public long getLastUpdatedMillis() {
        return this.lastUpdatedMillis;
    }

    public void setLastUpdatedMillis(long aLastUpdatedMillis) {
        this.lastUpdatedMillis = aLastUpdatedMillis;
    }

    public String getOwnerNodeId() {
        return ownerNodeId;
    }

    public void setOwnerNodeId(String anOwnerNodeId) {
        this.ownerNodeId = anOwnerNodeId;
    }

    public int getRemainingRetries() {
        return remainingRetries;
    }

    public void decrementRemainingRetries() {
        this.remainingRetries--;
    }

    @Override
    public String toString() {
        return String.format("TaskProcessingQueueItem [lastUpdatedMillis=%d, url=%s, ownerNodeId=%s, remainingRetries=%d]", lastUpdatedMillis, url, ownerNodeId, remainingRetries);
    }

    // only uses URL because we need to use that as a key for remove
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    // only uses URL because we need to use that as a key for remove
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaskProcessingQueueItem other = (TaskProcessingQueueItem) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    public void resetLastUpdatedMillis() {
        this.lastUpdatedMillis = System.currentTimeMillis();
    }
}
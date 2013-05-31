package com.bt.pi.core.application.watcher;

import com.bt.pi.core.entity.PiEntityBase;

public class WatcherQueryEntity extends PiEntityBase {
    private String queueUrl;
    private String entityUrl;

    public WatcherQueryEntity() {
    }

    public WatcherQueryEntity(String aQueueUrl, String anEntityUrl) {
        queueUrl = aQueueUrl;
        entityUrl = anEntityUrl;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return "wqe";
    }

    @Override
    public String getUrl() {
        return String.format("%s:%s:%s", getUriScheme(), queueUrl, entityUrl);
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getEntityUrl() {
        return entityUrl;
    }

    public void setQueueUrl(String aQueueUrl) {
        this.queueUrl = aQueueUrl;
    }

    public void setEntityUrl(String anEntityUrl) {
        this.entityUrl = anEntityUrl;
    }

    @Override
    public String toString() {
        return getUrl();
    }
}

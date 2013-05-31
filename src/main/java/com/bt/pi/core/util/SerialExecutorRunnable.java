package com.bt.pi.core.util;

public abstract class SerialExecutorRunnable implements Runnable {
    private String queueUrl;
    private String entityUrl;

    public SerialExecutorRunnable(String aQueueUrl, String anEntityUrl) {
        queueUrl = aQueueUrl;
        entityUrl = anEntityUrl;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public String getEntityUrl() {
        return entityUrl;
    }
}

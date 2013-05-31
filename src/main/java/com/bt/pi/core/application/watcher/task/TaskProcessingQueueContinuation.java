package com.bt.pi.core.application.watcher.task;

public interface TaskProcessingQueueContinuation {
    void receiveResult(String uri, String nodeId);
}

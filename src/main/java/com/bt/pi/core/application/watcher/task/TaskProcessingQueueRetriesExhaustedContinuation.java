package com.bt.pi.core.application.watcher.task;

public interface TaskProcessingQueueRetriesExhaustedContinuation {
    void receiveResult(String uri, String nodeId);
}

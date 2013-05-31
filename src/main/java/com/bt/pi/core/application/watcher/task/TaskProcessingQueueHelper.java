package com.bt.pi.core.application.watcher.task;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.id.PId;

/*
 * A Spring bean to help with managing TaskProcessingQueues
 */
@Component
public class TaskProcessingQueueHelper {
    protected static final int DEFAULT_REMAINING_RETRIES = -1;
    private static final String QUEUE_STATE_S = "queue state: %s";
    private static final Log LOG = LogFactory.getLog(TaskProcessingQueueHelper.class);
    private static final String DEFAULT_QUEUE_SIZE_WARNING_THRESHOLD = "100";
    private DhtClientFactory dhtClientFactory;
    private int queueSizeWarningThreshold = Integer.parseInt(DEFAULT_QUEUE_SIZE_WARNING_THRESHOLD);

    public TaskProcessingQueueHelper() {
        this.dhtClientFactory = null;
    }

    @Property(key = "queue.size.warning.threshold", defaultValue = DEFAULT_QUEUE_SIZE_WARNING_THRESHOLD)
    public void setQueueSizeWarningThreshold(int value) {
        this.queueSizeWarningThreshold = value;
    }

    public void addUrlToQueue(final PId queueId, final String url) {
        addUrlToQueue(queueId, url, DEFAULT_REMAINING_RETRIES, null);
    }

    public void addUrlToQueue(final PId queueId, final String url, TaskProcessingQueueContinuation continuation) {
        addUrlToQueue(queueId, url, DEFAULT_REMAINING_RETRIES, continuation);
    }

    public void addUrlToQueue(final PId queueId, final String url, final int remainingRetries) {
        addUrlToQueue(queueId, url, remainingRetries, null);
    }

    public void addUrlToQueue(final PId queueId, final String url, final int remainingRetries, final TaskProcessingQueueContinuation continuation) {
        LOG.debug(String.format("addUrlToQueue(%s, %s, %s, %s)", queueId, url, remainingRetries, continuation));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format("cannot add %s to non-existent queue %s", url, queueId.toStringFull()));
                    return null;
                }
                if (existingEntity.add(url, remainingRetries)) {
                    if (existingEntity.size() > queueSizeWarningThreshold) {
                        LOG.warn(String.format("queue %s has exceeded %d entries, size is now %d", queueId.toStringFull(), queueSizeWarningThreshold, existingEntity.size()));
                    }
                    return existingEntity;
                }
                return null;
            }

            @Override
            public void handleResult(TaskProcessingQueue result) {
                LOG.debug(String.format(QUEUE_STATE_S, result));
                if (null != continuation)
                    continuation.receiveResult(url, null);
            }
        });
    }

    public void removeUrlFromQueue(final PId queueId, final String url) {
        LOG.debug(String.format("removeUrlFromQueue(%s, %s)", queueId, url));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format("cannot remove %s from non-existent queue %s", url, queueId.toStringFull()));
                    return null;
                }
                if (existingEntity.remove(url))
                    return existingEntity;
                return null;
            }

            @Override
            public void handleResult(TaskProcessingQueue result) {
                LOG.debug(String.format(QUEUE_STATE_S, result));
            }
        });
    }

    public void setNodeIdOnUrl(final PId queueId, final String url, final String nodeId) {
        setNodeIdOnUrl(queueId, url, nodeId, null);
    }

    public void setNodeIdOnUrl(final PId queueId, final String url, final String nodeId, final TaskProcessingQueueContinuation continuation) {
        LOG.debug(String.format("setNodeIdOnUrl(%s, %s, %s, %s)", queueId, url, nodeId, continuation));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                if (null == existingEntity) {
                    LOG.warn(String.format("cannot update %s on non-existent queue %s", url, queueId));
                    return null;
                }
                existingEntity.setOwnerNodeIdForUrl(url, nodeId);
                return existingEntity;
            }

            @Override
            public void handleResult(TaskProcessingQueue result) {
                LOG.debug(String.format(QUEUE_STATE_S, result));
                if (null != continuation)
                    continuation.receiveResult(url, nodeId);
            }
        });
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }
}

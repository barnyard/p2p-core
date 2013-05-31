package com.bt.pi.core.application.watcher.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.application.watcher.WatcherApplication;
import com.bt.pi.core.application.watcher.WatcherQueryEntity;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.entity.TaskProcessingQueueItem;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.messaging.KoalaMessageContinuationException;

/*
 * keep an eye on a queue picking off items that are due for action
 */
public class TaskProcessingQueueWatcher implements Runnable {
    private static final int FIFTY = 50;
    private static final Log LOG = LogFactory.getLog(TaskProcessingQueueWatcher.class);
    private static final String TASK_WITH_IDENTIFIER_S_IS_QUEUED_OR_RUNNING_ALREADY_AT_WORKER_NODE = "Task with identifier %s is queued or running already at worker node";
    private static final String UNEXPECTED_EXCEPTION = "Unexpected exception";
    private static final String S_IS_STALE_AND_EXHAUSTED_AFTER_D_MILLI_SECONDS = "%s is stale and exhausted after %d milli-seconds";

    private PiLocation piLocation;
    private DhtClientFactory dhtClientFactory;
    private TaskProcessingQueueContinuation taskProcessingQueueContinuation;
    private long taskStaleMillis;
    private KoalaIdFactory koalaIdFactory;
    private TaskProcessingQueueRetriesExhaustedContinuation taskProcessingQueueRetriesExhaustedContinuation;
    private int numberOfItemsToProcess;
    private WatcherApplication watcherApplication;

    public TaskProcessingQueueWatcher(PiLocation aPiLocation, KoalaIdFactory aKoalaIdFactory, DhtClientFactory aDhtClientFactory, long timeoutMillis, int theNumberOfItemsToProcess, TaskProcessingQueueContinuation continuation,
            TaskProcessingQueueRetriesExhaustedContinuation exhaustedContinuation, WatcherApplication aWatcherApplication) {
        LOG.debug(String.format("TaskProcessingQueueWatcher(%s, %s, %s, %d, %d, %s, %s, %s)", aPiLocation, aKoalaIdFactory, aDhtClientFactory, timeoutMillis, theNumberOfItemsToProcess, continuation, exhaustedContinuation, aWatcherApplication));

        this.piLocation = aPiLocation;
        this.koalaIdFactory = aKoalaIdFactory;
        this.dhtClientFactory = aDhtClientFactory;
        this.taskStaleMillis = timeoutMillis;
        this.taskProcessingQueueContinuation = continuation;
        this.numberOfItemsToProcess = theNumberOfItemsToProcess;
        this.taskProcessingQueueRetriesExhaustedContinuation = exhaustedContinuation;
        this.watcherApplication = aWatcherApplication;
    }

    @Override
    public void run() {
        LOG.debug("run()");

        final PId queueId = this.koalaIdFactory.buildPId(this.piLocation.getUrl()).forLocalScope(this.piLocation.getNodeScope());
        DhtReader reader = dhtClientFactory.createReader();
        reader.getAsync(queueId, new PiContinuation<TaskProcessingQueue>() {
            @Override
            public void handleResult(TaskProcessingQueue result) {
                LOG.debug(String.format("handleResult(%s)", result));
                if (null == result) {
                    LOG.warn(String.format("%s queue not found, so not processing any URLs", piLocation.getUrl()));
                    return;
                }

                processQueueItems(queueId, result);
            }
        });
    }

    private Continuation<WatcherQueryEntity, Exception> getExhaustedRetriesContinuation(final PId queueId, final String url) {
        LOG.debug(String.format("getExhaustedRetriesContinuation(%s, %s", queueId, url));
        return new Continuation<WatcherQueryEntity, Exception>() {
            @Override
            public void receiveException(Exception exception) {
                if (exception instanceof KoalaMessageContinuationException && EntityResponseCode.NOT_FOUND.equals(((KoalaMessageContinuationException) exception).getEntityResponseCode())) {
                    LOG.info(String.format(S_IS_STALE_AND_EXHAUSTED_AFTER_D_MILLI_SECONDS, url, taskStaleMillis));
                    removeItemFromQueue();
                } else
                    LOG.error(UNEXPECTED_EXCEPTION, exception);
            }

            @Override
            public void receiveResult(WatcherQueryEntity result) {
                LOG.debug(String.format(TASK_WITH_IDENTIFIER_S_IS_QUEUED_OR_RUNNING_ALREADY_AT_WORKER_NODE, result));
            }

            private void removeItemFromQueue() {
                DhtWriter dhtWriter = dhtClientFactory.createWriter();
                dhtWriter.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
                    @Override
                    public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                        if (existingEntity.remove(url))
                            return existingEntity;

                        LOG.debug(String.format("Item %s already removed from queue", url));
                        return null;
                    }

                    @Override
                    public void handleResult(TaskProcessingQueue result) {
                        if (result != null) {
                            LOG.debug(String.format("Removed item %s from queue as all retries exhausted", url));
                            taskProcessingQueueRetriesExhaustedContinuation.receiveResult(url, watcherApplication.getNodeIdFull());
                        }
                    }
                });
            }
        };
    }

    private Continuation<WatcherQueryEntity, Exception> getRetriesContinuation(final PId queueId, final TaskProcessingQueueItem item) {
        LOG.debug(String.format("getRetriesContinuation(%s, %s", queueId, item));
        return new Continuation<WatcherQueryEntity, Exception>() {
            @Override
            public void receiveException(Exception exception) {
                if (exception instanceof KoalaMessageContinuationException && EntityResponseCode.NOT_FOUND.equals(((KoalaMessageContinuationException) exception).getEntityResponseCode())) {
                    LOG.info(String.format(S_IS_STALE_AND_EXHAUSTED_AFTER_D_MILLI_SECONDS, item.getUrl(), taskStaleMillis));
                    decrementItemInQueue(queueId, item);
                } else if (exception instanceof KoalaMessageContinuationException) {
                    PiEntity payload = ((KoalaMessageContinuationException) exception).getPayload();
                    LOG.error(String.format("%s, response code: %s, payload: %s", UNEXPECTED_EXCEPTION, ((KoalaMessageContinuationException) exception).getEntityResponseCode(),
                            StringUtils.abbreviate(null == payload ? null : payload.toString(), FIFTY)));
                } else
                    LOG.error(UNEXPECTED_EXCEPTION, exception);
            }

            @Override
            public void receiveResult(WatcherQueryEntity result) {
                LOG.debug(String.format(TASK_WITH_IDENTIFIER_S_IS_QUEUED_OR_RUNNING_ALREADY_AT_WORKER_NODE, result));
            }
        };
    }

    private void decrementItemInQueue(final PId queueId, final TaskProcessingQueueItem item) {
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                TaskProcessingQueueItem existingItem = existingEntity.get(item.getUrl());
                if (null == existingItem) // another thread beaten me to it?
                    return null;
                int remainingRetriesForExistingItem = existingItem.getRemainingRetries();
                if (remainingRetriesForExistingItem == item.getRemainingRetries()) {
                    if (remainingRetriesForExistingItem > 0) {
                        LOG.debug(String.format("Decrementing remaining retries for item %s and resetting stale timeout", item));
                        existingItem.decrementRemainingRetries();
                    }

                    existingItem.resetLastUpdatedMillis();
                    return existingEntity;
                } else
                    LOG.debug(String.format("Queue item %s already processed, so not decrementing number of retries", item));
                return null;
            }

            @Override
            public void handleResult(TaskProcessingQueue result) {
                if (result != null) {
                    LOG.debug(String.format("Decremented remaining retries for item %s and reset stale timeout", item));
                    taskProcessingQueueContinuation.receiveResult(item.getUrl(), watcherApplication.getNodeIdFull());
                }
            }
        });
    }

    private void processQueueItems(final PId queueId, TaskProcessingQueue existingEntity) {
        LOG.info(String.format("checking %s for stale tasks", piLocation.getUrl()));
        Collection<TaskProcessingQueueItem> staleItems = existingEntity.getStale(taskStaleMillis);
        Collection<TaskProcessingQueueItem> itemsToBeRemoved = new ArrayList<TaskProcessingQueueItem>();
        int itemsCount = 1;
        for (final TaskProcessingQueueItem item : staleItems) {
            if (itemsCount++ > numberOfItemsToProcess)
                break;

            Continuation<WatcherQueryEntity, Exception> continuation = null;
            int remainingRetries = item.getRemainingRetries();
            if (remainingRetries == 0) {
                if (null == taskProcessingQueueRetriesExhaustedContinuation) {
                    LOG.warn(String.format("%s is exhausted, but there is no appropriate continuation", item.getUrl()));
                    itemsToBeRemoved.add(item);
                    continue;
                }

                continuation = getExhaustedRetriesContinuation(queueId, item.getUrl());
            } else {
                continuation = getRetriesContinuation(queueId, item);
            }

            WatcherQueryEntity watcherQueryEntity = new WatcherQueryEntity(piLocation.getUrl(), item.getUrl());
            String ownerNodeId = item.getOwnerNodeId();
            if (ownerNodeId == null) {
                LOG.debug(String.format("No owner node id for item %s, so retrying", watcherQueryEntity));
                continuation.receiveException(new KoalaMessageContinuationException(EntityResponseCode.NOT_FOUND, watcherQueryEntity));
            } else {
                PId nodePid = koalaIdFactory.buildPIdFromHexString(ownerNodeId);

                LOG.debug(String.format("Sending message to node %s, asking if item %s is still being processed", item.getOwnerNodeId(), watcherQueryEntity));
                watcherApplication.newMessageContext(UUID.randomUUID().toString()).routePiMessage(nodePid, EntityMethod.GET, watcherQueryEntity, continuation);
            }
        }
        removeItems(itemsToBeRemoved);
    }

    private void removeItems(final Collection<TaskProcessingQueueItem> itemsToBeRemoved) {
        if (itemsToBeRemoved.size() < 1)
            return;
        final PId queueId = this.koalaIdFactory.buildPId(this.piLocation.getUrl()).forLocalScope(this.piLocation.getNodeScope());
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(queueId, new UpdateResolvingPiContinuation<TaskProcessingQueue>() {
            @Override
            public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) {
                if (null == existingEntity)
                    return null;
                boolean someRemoved = false;
                for (TaskProcessingQueueItem item : itemsToBeRemoved)
                    someRemoved |= existingEntity.remove(item.getUrl());
                if (someRemoved)
                    return existingEntity;
                return null;
            }

            @Override
            public void handleResult(TaskProcessingQueue result) {
                if (result != null)
                    LOG.debug(String.format("removed %d items from %s", itemsToBeRemoved.size(), piLocation.getUrl()));
            }
        });
    }
}

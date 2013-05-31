package com.bt.pi.core.application.watcher.task;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.core.application.MessageContext;
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
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class TaskProcessingQueueWatcherTest {
    private TaskProcessingQueueWatcher taskProcessingQueueWatcher;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private PId queueId;
    private PiLocation queue;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    private String itemUrl = "itemUrl";
    private long staleMillis = 10;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private TaskProcessingQueue taskProcessingQueue;
    private String nodeId = "99887666";
    private String queueUrl = "queueUrl";
    @Mock
    private TaskProcessingQueueItem taskProcessingQueueItem;
    @Mock
    private TaskProcessingQueueContinuation taskProcessingQueueContinuation;
    @Mock
    private TaskProcessingQueueRetriesExhaustedContinuation taskProcessingQueueRetriesExhaustedContinuation;
    @Mock
    private WatcherApplication watcherApplication;
    @Mock
    private MessageContext messageContext;
    @Mock
    private PId nodePid;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(watcherApplication.getNodeIdFull()).thenReturn(nodeId);
        this.queue = new PiLocation(queueUrl, NodeScope.AVAILABILITY_ZONE);
        when(this.koalaIdFactory.buildPId(queueUrl)).thenReturn(queueId);
        when(koalaIdFactory.buildPIdFromHexString(nodeId)).thenReturn(nodePid);
        this.taskProcessingQueueWatcher = new TaskProcessingQueueWatcher(queue, koalaIdFactory, dhtClientFactory, staleMillis, 5, taskProcessingQueueContinuation, taskProcessingQueueRetriesExhaustedContinuation, watcherApplication);

        when(queueId.forLocalScope(queue.getNodeScope())).thenReturn(queueId);
        when(this.dhtClientFactory.createReader()).thenReturn(this.dhtReader);
        when(this.dhtClientFactory.createWriter()).thenReturn(this.dhtWriter);
        when(taskProcessingQueueItem.getUrl()).thenReturn(this.itemUrl);
        when(taskProcessingQueueItem.getOwnerNodeId()).thenReturn(nodeId);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(taskProcessingQueueContinuation).receiveResult(eq(itemUrl), eq(nodeId));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        }).when(taskProcessingQueueRetriesExhaustedContinuation).receiveResult(eq(itemUrl), eq(nodeId));

        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(TaskProcessingQueueHelper.DEFAULT_REMAINING_RETRIES);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(taskProcessingQueue);
                return null;
            }
        }).when(this.dhtReader).getAsync(eq(queueId), isA(PiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object update = ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).update(taskProcessingQueue, taskProcessingQueue);
                ((UpdateResolvingPiContinuation) invocation.getArguments()[1]).receiveResult(update);
                return null;
            }
        }).when(dhtWriter).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));

        when(watcherApplication.newMessageContext(isA(String.class))).thenReturn(messageContext);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[3]).receiveException(new KoalaMessageContinuationException(EntityResponseCode.NOT_FOUND, (PiEntity) invocation.getArguments()[2]));
                return null;
            }
        }).when(messageContext).routePiMessage(isA(PId.class), eq(EntityMethod.GET), isA(WatcherQueryEntity.class), isA(Continuation.class));
    }

    @Test
    public void testThatRunCallsContinuationIfQueueReturnsAValue() throws InterruptedException {
        // setup
        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem));
        when(taskProcessingQueue.get(itemUrl)).thenReturn(taskProcessingQueueItem);
        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(7);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueueItem).decrementRemainingRetries();
        verify(taskProcessingQueueItem).resetLastUpdatedMillis();
        verify(taskProcessingQueueContinuation).receiveResult(itemUrl, nodeId);
    }

    @Test
    public void shouldHandleAnotherThreadRemovingItemFromQueueDuringRetryDecrement() throws InterruptedException {
        // setup
        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem));
        when(taskProcessingQueue.get(itemUrl)).thenReturn(null);
        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(7);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        // No exception plus...
        verify(taskProcessingQueueItem, never()).decrementRemainingRetries();
        verify(taskProcessingQueueItem, never()).resetLastUpdatedMillis();
        verify(taskProcessingQueueContinuation, never()).receiveResult(itemUrl, nodeId);
    }

    @Test
    public void testThatRunCallsExhaustedContinuationIfQueueReturnsAValueThatHasNoRetries() throws InterruptedException {
        // setup
        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem));
        when(taskProcessingQueue.remove(itemUrl)).thenReturn(true);

        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(0);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueue).remove(itemUrl);
        verify(taskProcessingQueueRetriesExhaustedContinuation).receiveResult(itemUrl, nodeId);
    }

    @Test
    public void testThatRunCallsContinuationMoreThanOnceIfQueueReturnsMultipleValue() throws InterruptedException {
        // setup
        TaskProcessingQueueItem taskProcessingQueueItem2 = newTaskProcessingItem("2");
        TaskProcessingQueueItem taskProcessingQueueItem3 = newTaskProcessingItem("3");

        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem, taskProcessingQueueItem2, taskProcessingQueueItem3));
        when(taskProcessingQueue.get(itemUrl)).thenReturn(taskProcessingQueueItem);
        when(taskProcessingQueue.get(taskProcessingQueueItem2.getUrl())).thenReturn(taskProcessingQueueItem2);
        when(taskProcessingQueue.get(taskProcessingQueueItem3.getUrl())).thenReturn(taskProcessingQueueItem3);

        when(taskProcessingQueueItem.getRemainingRetries()).thenReturn(8);
        when(taskProcessingQueueItem2.getRemainingRetries()).thenReturn(-1);
        when(taskProcessingQueueItem3.getRemainingRetries()).thenReturn(7);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueue, never()).remove(itemUrl);
        verify(taskProcessingQueueItem).decrementRemainingRetries();
        verify(taskProcessingQueueItem2, never()).decrementRemainingRetries();
        verify(taskProcessingQueueItem3).decrementRemainingRetries();
        verify(taskProcessingQueueContinuation).receiveResult(itemUrl, nodeId);
        verify(taskProcessingQueueContinuation).receiveResult(taskProcessingQueueItem2.getUrl(), nodeId);
        verify(taskProcessingQueueContinuation).receiveResult(taskProcessingQueueItem3.getUrl(), nodeId);
    }

    private TaskProcessingQueueItem newTaskProcessingItem(String append) {
        TaskProcessingQueueItem taskProcessingQueueItem = mock(TaskProcessingQueueItem.class);
        when(taskProcessingQueueItem.getUrl()).thenReturn(itemUrl + append);
        when(taskProcessingQueueItem.getOwnerNodeId()).thenReturn(nodeId);

        return taskProcessingQueueItem;
    }

    @Test
    public void testThatRunCallsBothContinuationsIfQueueReturnsMultipleValue() throws InterruptedException {
        // setup
        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(7);

        TaskProcessingQueueItem exhaustedItem = mock(TaskProcessingQueueItem.class);
        when(exhaustedItem.getUrl()).thenReturn(this.itemUrl);
        when(exhaustedItem.getRemainingRetries()).thenReturn(0);
        when(taskProcessingQueue.remove(itemUrl)).thenReturn(true);

        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem, exhaustedItem));
        when(taskProcessingQueue.get(itemUrl)).thenReturn(taskProcessingQueueItem);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueueItem).decrementRemainingRetries();
        verify(taskProcessingQueueItem).resetLastUpdatedMillis();
        verify(taskProcessingQueueContinuation).receiveResult(itemUrl, nodeId);
        verify(taskProcessingQueue).remove(itemUrl);
        verify(taskProcessingQueueRetriesExhaustedContinuation).receiveResult(itemUrl, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatRunFailsGracefullyIfQueueNotFound() throws InterruptedException {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.receiveResult(null);
                return null;
            }
        }).when(dhtReader).getAsync(eq(queueId), isA(PiContinuation.class));

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueue, never()).getStale(anyLong());
    }

    @Test
    public void testThatRunDoesNotTryAndCallNullExhaustedContinuation() throws InterruptedException {
        // setup
        this.taskProcessingQueueWatcher = new TaskProcessingQueueWatcher(queue, koalaIdFactory, dhtClientFactory, staleMillis, 5, taskProcessingQueueContinuation, null, watcherApplication);

        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem));
        when(taskProcessingQueue.remove(itemUrl)).thenReturn(true);
        when(this.taskProcessingQueueItem.getRemainingRetries()).thenReturn(0);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(taskProcessingQueue).remove(itemUrl);
        verify(taskProcessingQueueItem, never()).decrementRemainingRetries();
        verify(taskProcessingQueueContinuation, never()).receiveResult(itemUrl, nodeId);
        verify(taskProcessingQueueRetriesExhaustedContinuation, never()).receiveResult(itemUrl, nodeId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatRunCallsThroughDirectlyToContinuationIfOwnerNodeIdIsNull() throws Exception {
        // setup
        when(taskProcessingQueue.getStale(anyLong())).thenReturn(Arrays.asList(taskProcessingQueueItem));
        when(taskProcessingQueue.get(itemUrl)).thenReturn(taskProcessingQueueItem);
        when(taskProcessingQueueItem.getRemainingRetries()).thenReturn(7);
        when(taskProcessingQueueItem.getOwnerNodeId()).thenReturn(null);

        // act
        this.taskProcessingQueueWatcher.run();

        // assert
        verify(messageContext, never()).routePiMessage(isA(PId.class), eq(EntityMethod.GET), isA(WatcherQueryEntity.class), isA(Continuation.class));
        verify(taskProcessingQueueItem).decrementRemainingRetries();
        verify(taskProcessingQueueItem).resetLastUpdatedMillis();
        verify(taskProcessingQueueContinuation).receiveResult(itemUrl, nodeId);
    }
}

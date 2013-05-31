package com.bt.pi.core.application.watcher.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.TaskProcessingQueue;
import com.bt.pi.core.entity.TaskProcessingQueueItem;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class TaskProcessingQueueHelperTest {
    @InjectMocks
    private TaskProcessingQueueHelper taskProcessingQueueHelper = new TaskProcessingQueueHelper();
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private DhtClientFactory dhtClientFactory;
    private String url = "vol:vol-123";
    @Mock
    private PId queueId;
    @Mock
    private DhtWriter writer;
    private CountDownLatch latch;
    private String nodeId = "98348712387123";

    @Before
    public void setUp() throws Exception {
        when(this.koalaIdFactory.buildPId(isA(String.class))).thenReturn(queueId);
        when(this.dhtClientFactory.createWriter()).thenReturn(writer);
        latch = new CountDownLatch(1);
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testAddUrlToQueue() throws Exception {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myurl");

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));
        final CountDownLatch continuationLatch = new CountDownLatch(1);

        // act
        this.taskProcessingQueueHelper.addUrlToQueue(queueId, url, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                assertEquals(url, uri);
                assertNull(nodeId);
                continuationLatch.countDown();
            }
        });

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        Collection<TaskProcessingQueueItem> collection = taskProcessingQueue.getStale(-1);
        assertEquals(1, collection.size());
        TaskProcessingQueueItem pollStale = collection.iterator().next();
        assertEquals(url, pollStale.getUrl());
        assertEquals(TaskProcessingQueueHelper.DEFAULT_REMAINING_RETRIES, pollStale.getRemainingRetries());
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
    }

    @Test
    @SuppressWarnings( { "unchecked" })
    public void testAddExceedsWarningThreshold() throws Exception {
        // setup
        int threshold = 5;
        this.taskProcessingQueueHelper.setQueueSizeWarningThreshold(threshold);
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:url");
        latch = new CountDownLatch(threshold + 1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));

        final List<String> messages = new ArrayList<String>();
        AppenderSkeleton appender = new AppenderSkeleton() {

            @Override
            public boolean requiresLayout() {
                return false;
            }

            @Override
            public void close() {

            }

            @Override
            protected void append(LoggingEvent arg0) {
                if (!arg0.getLevel().equals(Level.WARN))
                    return;
                messages.add(arg0.getMessage().toString());
            }
        };
        LogManager.getRootLogger().addAppender(appender);

        // act
        for (int i = 0; i < threshold + 1; i++) {
            this.taskProcessingQueueHelper.addUrlToQueue(queueId, url + i);
        }

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        boolean found = false;
        for (String message : messages) {
            if (message.endsWith(String.format("%s has exceeded %d entries, size is now %d", queueId.toStringFull(), threshold, threshold + 1))) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testAddUrlToQueueAlreadyExists() throws Exception {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myqueue");
        taskProcessingQueue.add(url);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                Object updateResult = continuation.update(taskProcessingQueue, null);
                assertNull(updateResult);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));
        final CountDownLatch continuationLatch = new CountDownLatch(1);

        // act
        this.taskProcessingQueueHelper.addUrlToQueue(queueId, url, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                assertEquals(url, uri);
                assertNull(nodeId);
                continuationLatch.countDown();
            }
        });

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testAddUrlToQueueWithRemainingRetries() throws Exception {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myqueue");

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));
        int retries = 44;
        final CountDownLatch continuationLatch = new CountDownLatch(1);

        // act
        this.taskProcessingQueueHelper.addUrlToQueue(queueId, url, retries, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                assertEquals(url, uri);
                assertNull(nodeId);
                continuationLatch.countDown();
            }
        });

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        Collection<TaskProcessingQueueItem> collection = taskProcessingQueue.getStale(-1);
        assertEquals(1, collection.size());
        TaskProcessingQueueItem pollStale = collection.iterator().next();
        assertEquals(url, pollStale.getUrl());
        assertEquals(retries, pollStale.getRemainingRetries());
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void shouldReturnNullIfNotFound() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                TaskProcessingQueue taskProcessingQueue = (TaskProcessingQueue) continuation.update(null, null);
                // assert
                assertNull(taskProcessingQueue);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));
        final CountDownLatch continuationLatch = new CountDownLatch(1);

        // act
        this.taskProcessingQueueHelper.addUrlToQueue(queueId, url, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String nodeId) {
                assertEquals(url, uri);
                assertNull(nodeId);
                continuationLatch.countDown();
            }
        });

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testRemove() throws Exception {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myqueue");
        taskProcessingQueue.add(url);
        String url2 = "vol:vol-9999";
        taskProcessingQueue.add(url2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));

        // act
        this.taskProcessingQueueHelper.removeUrlFromQueue(queueId, url2);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        Collection<TaskProcessingQueueItem> collection = taskProcessingQueue.getStale(-1);
        assertEquals(1, collection.size());
        assertEquals(url, collection.iterator().next().getUrl());
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testAddNodeIdToUrl() throws InterruptedException {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myqueue");
        taskProcessingQueue.add(url);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));

        // act
        this.taskProcessingQueueHelper.setNodeIdOnUrl(queueId, url, nodeId);

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(nodeId, taskProcessingQueue.getNodeIdForUrl(url));
    }

    @SuppressWarnings( { "unchecked" })
    @Test
    public void testAddNodeIdToUrlWithContinuation() throws InterruptedException {
        // setup
        final TaskProcessingQueue taskProcessingQueue = new TaskProcessingQueue("queue:myqueue");
        taskProcessingQueue.add(url);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation continuation = (UpdateResolvingPiContinuation) invocation.getArguments()[1];
                continuation.update(taskProcessingQueue, null);
                continuation.receiveResult(taskProcessingQueue);
                latch.countDown();
                return null;
            }
        }).when(writer).update(eq(queueId), isA(UpdateResolvingPiContinuation.class));
        final CountDownLatch continuationLatch = new CountDownLatch(1);

        // act
        this.taskProcessingQueueHelper.setNodeIdOnUrl(queueId, url, nodeId, new TaskProcessingQueueContinuation() {
            @Override
            public void receiveResult(String uri, String aNodeId) {
                assertEquals(url, uri);
                assertEquals(nodeId, aNodeId);
                continuationLatch.countDown();
            }
        });

        // assert
        assertTrue(latch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(continuationLatch.await(200, TimeUnit.MILLISECONDS));
        assertEquals(nodeId, taskProcessingQueue.getNodeIdForUrl(url));
    }
}

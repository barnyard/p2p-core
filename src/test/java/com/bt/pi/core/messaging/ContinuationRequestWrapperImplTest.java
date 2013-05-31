//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public class ContinuationRequestWrapperImplTest {
    private ContinuationRequestWrapperImpl continuationRequestWrapperImpl;
    private PId mockId;
    private Continuation<KoalaMessage, Exception> mockContinuation;
    private KoalaMessage koalaMessage;
    private KoalaMessageSender koalaMessageSender;
    private WatcherService watcherService;
    private CountDownLatch countDownLatch;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        mockId = mock(PId.class);
        mockContinuation = mock(Continuation.class);
        koalaMessageSender = mock(KoalaMessageSender.class);
        watcherService = mock(WatcherService.class);
        countDownLatch = new CountDownLatch(1);

        koalaMessage = mock(KoalaMessage.class);
        when(koalaMessage.getCorrelationUID()).thenReturn(UUID.randomUUID().toString());

        continuationRequestWrapperImpl = new ContinuationRequestWrapperImpl() {
            @Override
            protected long getNow() {
                return System.currentTimeMillis() + 5000;
            }
        };
        continuationRequestWrapperImpl.setHousekeepIntervalMillis(1);
        continuationRequestWrapperImpl.setMessageTimeToLiveMillis(1);
        continuationRequestWrapperImpl.setWatcherService(watcherService);
    }

    @Test
    public void constructorTest() {
        assertNotNull(new ContinuationRequestWrapperImpl());
    }

    @Test
    public void testSendRequest() {
        // act
        continuationRequestWrapperImpl.sendRequest(mockId, koalaMessage, koalaMessageSender, mockContinuation);

        // verify
        verify(koalaMessageSender).routeMessage(eq(mockId), eq(koalaMessage));
    }

    @Test
    public void testMessageReceived() {
        // setup
        continuationRequestWrapperImpl.sendRequest(mockId, koalaMessage, koalaMessageSender, mockContinuation);
        when(koalaMessage.getResponseCode()).thenReturn(EntityResponseCode.OK);

        // act
        continuationRequestWrapperImpl.messageReceived(mockId, koalaMessage);

        // verify
        verify(mockContinuation).receiveResult(koalaMessage);
    }

    @Test
    public void testRequestReceivedIsNotTreatedAsResponse() {
        // setup
        continuationRequestWrapperImpl.sendRequest(mockId, koalaMessage, koalaMessageSender, mockContinuation);
        when(koalaMessage.getResponseCode()).thenReturn(null);

        // act
        continuationRequestWrapperImpl.messageReceived(mockId, koalaMessage);

        // verify
        verify(mockContinuation, never()).receiveResult(koalaMessage);
    }

    @Test
    public void testMessageHousekeeper() {
        // setup
        continuationRequestWrapperImpl.sendRequest(mockId, koalaMessage, koalaMessageSender, mockContinuation);

        // act
        continuationRequestWrapperImpl.housekeepStaleMessages();

        // assert
        assertEquals(0, continuationRequestWrapperImpl.getContinuationMap().size());
    }

    @Test
    public void initialisationShouldScheduleHousekeeperWithWatcherService() {
        // act
        continuationRequestWrapperImpl.initialise();

        // assert
        verify(watcherService).addTask(eq(continuationRequestWrapperImpl.toString()), isA(Runnable.class), eq(1000L), eq(1000L));
    }

    @Test
    public void housekeeperShoulBeRunByWatcherService() throws InterruptedException {
        // setup
        watcherService = new WatcherService();
        watcherService.setScheduledExecutorService(Executors.newScheduledThreadPool(4));
        watcherService.start();

        continuationRequestWrapperImpl = new ContinuationRequestWrapperImpl() {
            @Override
            protected void housekeepStaleMessages() {
                super.housekeepStaleMessages();
                countDownLatch.countDown();
            }
        };
        continuationRequestWrapperImpl.setWatcherService(watcherService);
        continuationRequestWrapperImpl.setHousekeepIntervalMillis(1);
        continuationRequestWrapperImpl.setMessageTimeToLiveMillis(1);

        continuationRequestWrapperImpl.sendRequest(mockId, koalaMessage, koalaMessageSender, mockContinuation);
        assertEquals(1, continuationRequestWrapperImpl.getContinuationMap().size());

        // act
        continuationRequestWrapperImpl.initialise();

        // assert
        assertTrue(countDownLatch.await(5, TimeUnit.SECONDS));
        assertEquals(0, continuationRequestWrapperImpl.getContinuationMap().size());
    }
}

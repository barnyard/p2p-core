package com.bt.pi.core.application.watcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.watcher.WatcherApplication;
import com.bt.pi.core.application.watcher.WatcherQueryEntity;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.util.SerialExecutor;

@RunWith(MockitoJUnitRunner.class)
public class WatcherApplicationTest {
    private String queue = "id";
    private String entity = "id2";
    private PiEntity watcherQueryEntity = new WatcherQueryEntity(queue, entity);

    @Mock
    PId id;
    @Mock
    ReceivedMessageContext receivedMessageContext;

    @Mock
    private AlwaysOnApplicationActivator alwaysOnApplicationActivator;
    @Mock
    private SerialExecutor serialExecutor;

    @InjectMocks
    private WatcherApplication watcherApplication = new WatcherApplication();

    @Before
    public void setup() {
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.GET);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(watcherQueryEntity);
    }

    @Test
    public void testApplicationActivatorIsAlwaysOn() throws Exception {
        // act
        ApplicationActivator result = watcherApplication.getApplicationActivator();

        assertEquals(alwaysOnApplicationActivator, result);
    }

    @Test
    public void testThatBecomeActiveAlwaysReturnsTrue() throws Exception {
        // act
        boolean result = watcherApplication.becomeActive();

        // assert
        assertTrue(result);
    }

    @Test
    public void testThatApplicationNameIsReturned() throws Exception {
        // act
        String result = watcherApplication.getApplicationName();

        // assert
        assertEquals("watcherApplication", result);
    }

    @Test
    public void testActivationCheckTimeout() throws Exception {
        // act
        int result = watcherApplication.getActivationCheckPeriodSecs();

        // assert
        assertEquals(60, result);
    }

    @Test
    public void testStartTimeout() throws Exception {
        // act
        long result = watcherApplication.getStartTimeout();

        // assert
        assertEquals(1, result);
    }

    @Test
    public void testStartTimeoutUnit() throws Exception {
        // act
        TimeUnit result = watcherApplication.getStartTimeoutUnit();

        // assert
        assertEquals(TimeUnit.SECONDS, result);
    }

    @Test
    public void testThatWatcherRespondsWithYesIfNodeIsWorkingOnQueuedTask() throws Exception {
        // setup
        when(serialExecutor.isQueuedOrRunning(queue, entity)).thenReturn(true);

        // act
        watcherApplication.deliver(id, receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(EntityResponseCode.OK, watcherQueryEntity);
    }

    @Test
    public void testThatWatcherRespondsWithNoIfNodeIsNotWorkingOnQueuedTask() throws Exception {
        // act
        watcherApplication.deliver(id, receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(EntityResponseCode.NOT_FOUND, watcherQueryEntity);
    }

    @Test
    public void testThatWatcherRespondsWithErrorIfEntityIsNotWatcherQueryEntity() throws Exception {
        // setup
        EchoPayload piEntity = new EchoPayload();
        when(receivedMessageContext.getReceivedEntity()).thenReturn(piEntity);

        // act
        watcherApplication.deliver(id, receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(EntityResponseCode.ERROR, piEntity);
    }

    @Test
    public void testThatWatcherRespondsWithErrorIfEntityIsNull() throws Exception {
        // setup
        when(receivedMessageContext.getReceivedEntity()).thenReturn(null);

        // act
        watcherApplication.deliver(id, receivedMessageContext);

        // assert
        verify(receivedMessageContext).sendResponse(EntityResponseCode.ERROR, null);
    }
}

package com.bt.pi.core.application.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.testing.TestFriendlyContinuation;

@RunWith(MockitoJUnitRunner.class)
public class AbstractConsumedResourceRegistryTest {
    private static final String RESOURCE_ID = "resource-id";
    private TestFriendlyContinuation<Boolean> testContinuation = new TestFriendlyContinuation<Boolean>();
    private AbstractConsumedResourceRegistry<String> abstractConsumedResourceRegistry;
    @Mock
    private WatcherService watcherService;
    @Mock
    private SharedResourceWatchingStrategy<String> sharedResourceWatchingStrategy;
    @Mock
    private Runnable resourceRefreshRunner;
    @Mock
    private Runnable consumerWatcher;

    @Before
    public void before() {
        when(sharedResourceWatchingStrategy.getSharedResourceRefreshRunner(anyString())).thenReturn(resourceRefreshRunner);
        when(sharedResourceWatchingStrategy.getConsumerWatcher(anyString(), anyString())).thenReturn(consumerWatcher);

        abstractConsumedResourceRegistry = new AbstractConsumedResourceRegistry<String>() {
            @Override
            protected String getKeyAsString(String resourceId) {
                return resourceId;
            }

            @Override
            protected SharedResourceWatchingStrategy<String> createNoOpResourceWatchingStrategy() {
                return sharedResourceWatchingStrategy;
            }
        };
        abstractConsumedResourceRegistry.setWatcherService(watcherService);
    }

    @Test
    public void shouldRegisterInitialConsumerAndAddWatchers() {
        // act
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);

        // assert
        assertTrue(testContinuation.lastResult);
        assertEquals(1, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-resource-refresh-runner"), eq(resourceRefreshRunner), anyLong(), anyLong());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-" + "one-consumer-watcher"), eq(consumerWatcher), anyLong(), anyLong());
    }

    @Test
    public void shouldRegisterSecondConsumerAndAddWatchers() {
        // setup
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);

        // act
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "two", testContinuation);

        // assert
        assertFalse(testContinuation.lastResult);
        assertEquals(2, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-resource-refresh-runner"), eq(resourceRefreshRunner), anyLong(), anyLong());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-" + "one-consumer-watcher"), eq(consumerWatcher), anyLong(), anyLong());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-" + "two-consumer-watcher"), eq(consumerWatcher), anyLong(), anyLong());
    }

    @Test
    public void shouldNoOpWhenReRegisteringConsumer() {
        // setup
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "two", testContinuation);

        // act
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);

        // assert
        assertFalse(testContinuation.lastResult);
        assertEquals(2, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-resource-refresh-runner"), eq(resourceRefreshRunner), anyLong(), anyLong());
        verify(watcherService, times(2)).replaceTask(eq(RESOURCE_ID + "-" + "one-consumer-watcher"), eq(consumerWatcher), anyLong(), anyLong());
        verify(watcherService).replaceTask(eq(RESOURCE_ID + "-" + "two-consumer-watcher"), eq(consumerWatcher), anyLong(), anyLong());
    }

    @Test
    public void shouldRegisterDifferentResourceWithSameConsumer() {
        // setup
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "two", testContinuation);

        // act
        abstractConsumedResourceRegistry.registerConsumer("some-other-resource", "one", testContinuation);

        // assert
        assertTrue(testContinuation.lastResult);
        assertEquals(2, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        assertEquals(1, abstractConsumedResourceRegistry.getAllConsumers("some-other-resource").size());
    }

    @Test
    public void shouldDeregisterLastRemainingConsumerAndRemoveWatchers() {
        // setup
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);

        // act
        boolean res = abstractConsumedResourceRegistry.deregisterConsumer(RESOURCE_ID, "one");

        // assert
        assertTrue(res);
        assertEquals(0, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        verify(watcherService).removeTask(eq(RESOURCE_ID + "-resource-refresh-runner"));
        verify(watcherService).removeTask(eq(RESOURCE_ID + "-" + "one-consumer-watcher"));
    }

    @Test
    public void shouldDeregisterNonFinalConsumer() {
        // setup
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "one", testContinuation);
        abstractConsumedResourceRegistry.registerConsumer(RESOURCE_ID, "two", testContinuation);

        // act
        boolean res = abstractConsumedResourceRegistry.deregisterConsumer(RESOURCE_ID, "one");

        // assert
        assertFalse(res);
        assertEquals(1, abstractConsumedResourceRegistry.getAllConsumers(RESOURCE_ID).size());
        verify(watcherService, never()).removeTask(eq(RESOURCE_ID + "-resource-refresh-runner"));
        verify(watcherService).removeTask(eq(RESOURCE_ID + "-" + "one-consumer-watcher"));
    }
}

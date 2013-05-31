package com.bt.pi.core.application.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;

public class DefaultDhtResourceWatchingStrategyTest {
    private DefaultDhtResourceWatchingStrategy watchingStrategy;
    private CachingConsumedResourceRegistry cachingResourceManager;
    private PId aaId;

    @Before
    public void before() {
        aaId = new PiId("aaaaaaaaaaaaaaaaaaaaaa", 0);

        cachingResourceManager = mock(CachingConsumedResourceRegistry.class);
        watchingStrategy = new DefaultDhtResourceWatchingStrategy();
        watchingStrategy.setCachingConsumedResourceRegistry(cachingResourceManager);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowIfGettingUnsetResManager() {
        // setup
        watchingStrategy = new DefaultDhtResourceWatchingStrategy();

        // act
        watchingStrategy.getCachingConsumedResourceRegistry();
    }

    @Test
    public void shouldGetDefaultRefreshRunner() {
        // act
        Runnable res = watchingStrategy.getSharedResourceRefreshRunner(aaId);

        // assert
        assertTrue(res instanceof DefaultDhtResourceRefreshRunner);
    }

    @Test
    public void shouldGetDefaultConsumerWatcher() {
        // act
        Runnable res = watchingStrategy.getConsumerWatcher(aaId, "bbb");

        // assert
        assertNull(res);
    }

    @Test
    public void checkDefaultConfigSettings() {
        // act/assert
        assertEquals(300000, watchingStrategy.getInitialResourceRefreshIntervalMillis());
        assertEquals(240000, watchingStrategy.getInitialConsumerWatcherIntervalMillis());
        assertEquals(600000, watchingStrategy.getRepeatingResourceRefreshIntervalMillis());
        assertEquals(660000, watchingStrategy.getRepeatingConsumerWatcherIntervalMillis());
    }
}

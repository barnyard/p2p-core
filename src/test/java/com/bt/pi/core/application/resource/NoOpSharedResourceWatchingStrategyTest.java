package com.bt.pi.core.application.resource;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.resource.NoOpSharedResourceWatchingStrategy;

import rice.p2p.commonapi.Id;

public class NoOpSharedResourceWatchingStrategyTest {
    private NoOpSharedResourceWatchingStrategy<Id> noOpSharedResourceWatchingStrategy;
    private Id resourceId;
    private String consumerId;

    @Before
    public void before() {
        noOpSharedResourceWatchingStrategy = new NoOpSharedResourceWatchingStrategy<Id>();
    }

    @Test
    public void coverage() {
        // assert
        assertEquals(null, noOpSharedResourceWatchingStrategy.getSharedResourceRefreshRunner(resourceId));
        assertEquals(null, noOpSharedResourceWatchingStrategy.getConsumerWatcher(resourceId, consumerId));
        assertEquals(0, noOpSharedResourceWatchingStrategy.getInitialResourceRefreshIntervalMillis());
        assertEquals(0, noOpSharedResourceWatchingStrategy.getRepeatingResourceRefreshIntervalMillis());
        assertEquals(0, noOpSharedResourceWatchingStrategy.getInitialConsumerWatcherIntervalMillis());
        assertEquals(0, noOpSharedResourceWatchingStrategy.getRepeatingConsumerWatcherIntervalMillis());
        noOpSharedResourceWatchingStrategy.setInitialResourceRefreshIntervalMillis(0);
        noOpSharedResourceWatchingStrategy.setRepeatingResourceRefreshIntervalMillis(0);
        noOpSharedResourceWatchingStrategy.setInitialConsumerWatcherIntervalMillis(0);
        noOpSharedResourceWatchingStrategy.setRepeatingConsumerWatcherIntervalMillis(0);
    }
}

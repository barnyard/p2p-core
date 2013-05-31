package com.bt.pi.core.node;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class NodeStartedEventTest {
    @Test
    public void testNotNull() throws Exception {
        // act
        NodeStartedEvent nodeStartedEvent = new NodeStartedEvent(this);

        // assert
        assertNotNull(nodeStartedEvent);
    }
}

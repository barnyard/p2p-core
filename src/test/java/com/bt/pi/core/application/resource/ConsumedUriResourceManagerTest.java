package com.bt.pi.core.application.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.continuation.LoggingContinuation;

public class ConsumedUriResourceManagerTest {
    private ConsumedUriResourceRegistry consumedUriResourceRegistry;
    private URI resourceId;

    @Before
    public void setup() {
        consumedUriResourceRegistry = new ConsumedUriResourceRegistry();
        resourceId = URI.create("res:1");
    }

    @Test
    public void testGetKeyAsString() throws Exception {
        // assert
        assertEquals(resourceId.toString(), consumedUriResourceRegistry.getKeyAsString(resourceId));
    }

    @Test
    public void getSetBySearchingOnUriScheme() throws Exception {
        // setup
        consumedUriResourceRegistry.registerConsumer(resourceId, "consumer", new LoggingContinuation<Boolean>());
        consumedUriResourceRegistry.registerConsumer(URI.create("nores:2"), "consumer", new LoggingContinuation<Boolean>());
        consumedUriResourceRegistry.registerConsumer(URI.create("res:3"), "consumer", new LoggingContinuation<Boolean>());

        // act
        Set<URI> result = consumedUriResourceRegistry.getResourceIdsByScheme("res");

        // assert
        assertEquals(2, result.size());
        assertTrue(result.contains(resourceId));
        assertTrue(result.contains(URI.create("res:3")));
    }

    @Test
    public void getSetBySearchingWithNullSchemeReturnsEmptySet() throws Exception {
        // act
        Set<URI> result = consumedUriResourceRegistry.getResourceIdsByScheme(null);

        // assert
        assertTrue(result.isEmpty());
    }
}

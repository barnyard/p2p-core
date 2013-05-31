package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.scope.NodeScope;

public class AvailabilityZoneScopedSharedRecordConditionalApplicationActivatorTest {
    private AvailabilityZoneScopedSharedRecordConditionalApplicationActivator activator;

    @Before
    public void before() {
        activator = new AvailabilityZoneScopedSharedRecordConditionalApplicationActivator();
    }

    @Test
    public void shouldHaveCorrectScope() {
        assertEquals(NodeScope.AVAILABILITY_ZONE, activator.getActivationScope());
    }
}

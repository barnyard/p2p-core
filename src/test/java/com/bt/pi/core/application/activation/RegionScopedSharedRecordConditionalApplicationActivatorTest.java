package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.scope.NodeScope;

public class RegionScopedSharedRecordConditionalApplicationActivatorTest {
    private RegionScopedSharedRecordConditionalApplicationActivator activator;

    @Before
    public void before() {
        activator = new RegionScopedSharedRecordConditionalApplicationActivator();
    }

    @Test
    public void shouldHaveCorrectScope() {
        assertEquals(NodeScope.REGION, activator.getActivationScope());
    }
}

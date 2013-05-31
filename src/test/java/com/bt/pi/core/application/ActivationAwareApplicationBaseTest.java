package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import rice.pastry.PastryNode;

import com.bt.pi.core.application.activation.ApplicationActivator;

public class ActivationAwareApplicationBaseTest {

    private class MyActivationAwareApplicationBase extends ActivationAwareApplicationBase {
        @Override
        public TimeUnit getStartTimeoutUnit() {
            return null;
        }

        @Override
        public long getStartTimeout() {
            return 0;
        }

        @Override
        public int getActivationCheckPeriodSecs() {
            return 0;
        }

        @Override
        public void becomePassive() {
        }

        @Override
        public boolean becomeActive() {
            return true;
        }

        @Override
        public String getApplicationName() {
            return null;
        }

        @Override
        public ApplicationActivator getApplicationActivator() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    private static final rice.pastry.Id NODE_ID = rice.pastry.Id.build("testNode");

    private ActivationAwareApplicationBase activationAwareApplicationBase;
    private PastryNode pastryNode;

    @Before
    public void before() {
        activationAwareApplicationBase = new MyActivationAwareApplicationBase();

        pastryNode = mock(PastryNode.class);
        activationAwareApplicationBase.start(pastryNode);

        when(pastryNode.getId()).thenReturn(NODE_ID);
    }

    @Test
    public void getMutuallyExclusiveApplicationsShouldReturnEmptyList() {
        assertTrue("mutuallyExclusiveApplications should be empty", activationAwareApplicationBase.getPreferablyExcludedApplications().isEmpty());
    }

    @Test
    public void getNodeIdShouldReturnIdFromPastryNode() {
        assertEquals(NODE_ID, activationAwareApplicationBase.getNodeId());
    }

    @Test
    public void getPastryNodeShouldReturnPastryNodeField() {
        assertTrue(pastryNode == activationAwareApplicationBase.getPastryNode());
    }

}

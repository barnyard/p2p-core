package com.bt.pi.core.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import rice.p2p.commonapi.Id.Distance;
import rice.pastry.Id;

public class SuperNodeIdFactoryTest {

    private int numberOfSuperNodes = 4;
    private String originalId = "000102030405060708090A0B0C0D0E0F10111213";

    @Test
    public void testGetGlobalSuperNodes() {

        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(numberOfSuperNodes, 1));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
    }

    @Test
    public void testGloabalSuperNodesWithBaseId() {
        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(numberOfSuperNodes, 1, originalId));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
        assertNodesContain(nodeIds, originalId.substring(2));
    }

    @Test
    public void testGetRegionalSuperNodes() {
        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(1, numberOfSuperNodes, 1));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
        assertNodesBeginWith(nodeIds, "01");
    }

    @Test
    public void testGetRegionalSuperNodesWithBaseId() {
        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(1, numberOfSuperNodes, 1, originalId));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
        assertNodesBeginWith(nodeIds, "01");
        assertNodesContain(nodeIds, originalId.substring(4));
    }

    @Test
    public void testGetAvailabilityZoneSuperNodes() {
        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(1, 1, numberOfSuperNodes, 1));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
        assertNodesBeginWith(nodeIds, "0101");
    }

    @Test
    public void testGetAvailabilityZoneSuperNodesWithBaseId() {
        // act
        ArrayList<String> nodeIds = new ArrayList<String>(SuperNodeIdFactory.getSuperNodeCheckPoints(1, 1, numberOfSuperNodes, 1, originalId));

        // assert
        assertEquals(numberOfSuperNodes, nodeIds.size());
        assertNodeIdsEquallySpaced(nodeIds);
        assertNodesBeginWith(nodeIds, "0101");
        assertNodesContain(nodeIds, originalId.substring(6));
    }

    private void assertNodesBeginWith(ArrayList<String> nodeIds, String str) {
        for (String nodeId : nodeIds) {
            assertTrue(nodeId.startsWith(str));
        }
    }

    private void assertNodesContain(ArrayList<String> nodeIds, String str) {
        for (String nodeId : nodeIds) {
            assertTrue(nodeId.contains(str));
        }
    }

    private void assertNodeIdsEquallySpaced(ArrayList<String> nodeIds) {
        Distance nodeDistance = Id.build(nodeIds.get(0)).distanceFromId(Id.build(nodeIds.get(1)));
        Id previousId = null;
        for (String nodeId : nodeIds) {
            Id currentId = Id.build(nodeId);
            if (previousId != null) {
                assertEquals(nodeDistance, previousId.distanceFromId(currentId));
            }
            previousId = currentId;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionThrownWhenRequestingNonPowerOfTwo() {

        SuperNodeIdFactory.getSuperNodeCheckPoints(1, 1, 5, 1, "bob");
    }

    @Test
    public void testReturnsEmptySetWhenRequestingZero() {
        assertEquals(SuperNodeIdFactory.getSuperNodeCheckPoints(1, 1, 0, 1, "bob").size(), 0);
    }
}

package com.bt.pi.core.id;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class KoalaIdUtilsTest {
    private KoalaIdUtils koalaIdUtils = new KoalaIdUtils();
    private Id idToCheck = rice.pastry.Id.build("12345678");
    private String nodeIdFull1 = "2345678923456789234567892345678923456789";
    private String nodeIdFull2 = "3456789034567890345678903456789034567890";
    private String nodeIdFull3 = "4567890145678901456789014567890145678901";
    private SortedSet<String> nodeIds = new TreeSet<String>(Arrays.asList(new String[] { nodeIdFull1, nodeIdFull2, nodeIdFull3 }));

    private String myId = "1235999654535383867383657323857328438924";

    @Mock
    private NodeHandle nodeHandle1;
    @Mock
    private NodeHandle nodeHandle2;
    @Mock
    private NodeHandle nodeHandle3;

    @Mock
    private Id nodeId1;
    @Mock
    private Id nodeId2;
    @Mock
    private Id nodeId3;
    private Collection<NodeHandle> leafNodeHandles;

    @Before
    public void setup() throws Exception {
        when(nodeHandle1.getId()).thenReturn(nodeId1);
        when(nodeHandle2.getId()).thenReturn(nodeId2);
        when(nodeHandle3.getId()).thenReturn(nodeId3);
        when(nodeId1.toStringFull()).thenReturn(nodeIdFull1);
        when(nodeId2.toStringFull()).thenReturn(nodeIdFull2);
        when(nodeId3.toStringFull()).thenReturn(nodeIdFull3);
        leafNodeHandles = new ArrayList<NodeHandle>(Arrays.asList(new NodeHandle[] { nodeHandle1, nodeHandle2, nodeHandle3 }));
    }

    @Test
    public void testClosestIdIsNullIfNullSet() throws Exception {
        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(null, idToCheck);

        // assert
        assertNull(result);
    }

    @Test
    public void testClosestIdIsNullIfEmptySet() throws Exception {
        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(new TreeSet<String>(), idToCheck);

        // assert
        assertNull(result);
    }

    @Test
    public void testCLosestIdReturnsIfOnlyOneNodeInSet() throws Exception {
        // setup
        nodeIds.removeAll(Arrays.asList(nodeIdFull1, nodeIdFull2));

        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull3));
    }

    @Test
    public void testCLosestIdReturnsCorrectNode() throws Exception {
        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull1));
    }

    @Test
    public void testIsIdClosestToMeTrue() {
        // setup

        // act
        boolean result = koalaIdUtils.isIdClosestToMe(myId, leafNodeHandles, idToCheck);

        // assert
        assertTrue(result);
    }

    @Test
    public void testIsIdClosestToMeFalse() {
        // setup
        Id someOtherId = rice.pastry.Id.build("99995678");

        // act
        boolean result = koalaIdUtils.isIdClosestToMe(myId, leafNodeHandles, someOtherId);

        // assert
        assertFalse(result);
    }

    @Test
    public void testGetPositionFromId() {
        // setup
        idToCheck = rice.pastry.Id.build("1");
        when(nodeHandle1.getId()).thenReturn(rice.pastry.Id.build("3"));
        when(nodeHandle2.getId()).thenReturn(rice.pastry.Id.build("5"));
        when(nodeHandle3.getId()).thenReturn(rice.pastry.Id.build("7"));
        ArrayList<Id> ids = new ArrayList<Id>();
        ids.add(rice.pastry.Id.build("2"));
        ids.add(rice.pastry.Id.build("4"));
        ids.add(rice.pastry.Id.build("6"));
        ids.add(rice.pastry.Id.build("8"));

        for (int i = 0; i < ids.size(); i++) {
            int result = KoalaIdUtils.getPositionFromId(ids.get(i), leafNodeHandles, idToCheck);
            assertEquals(i, result);
        }
    }

    @Test
    public void testIsBackupId() {
        // setup
        String nodeIdFull4 = "234567892345678923456789234567892345678A";
        String nodeIdFull5 = "345678903456789034567890345678903456789B";
        String nodeIdFull6 = "456789014567890145678901456789014567890C";
        Id nodeId4 = mock(Id.class);
        Id nodeId5 = mock(Id.class);
        Id nodeId6 = mock(Id.class);
        when(nodeId4.toStringFull()).thenReturn(nodeIdFull4);
        when(nodeId5.toStringFull()).thenReturn(nodeIdFull5);
        when(nodeId6.toStringFull()).thenReturn(nodeIdFull6);

        // act
        assertTrue(KoalaIdUtils.isBackupId(nodeId1));
        assertFalse(KoalaIdUtils.isBackupId(nodeId2));
        assertTrue(KoalaIdUtils.isBackupId(nodeId3));
        assertFalse(KoalaIdUtils.isBackupId(nodeId4));
        assertTrue(KoalaIdUtils.isBackupId(nodeId5));
        assertFalse(KoalaIdUtils.isBackupId(nodeId6));
    }

    @Test
    public void testThatClosestReturnsNullIfNoNodesInAvzScope() throws Exception {
        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.AVAILABILITY_ZONE);

        // assert
        assertNull(result);
    }

    @Test
    public void testThatClosestChecksForClosestInAvzScope() throws Exception {
        String nodeIdFull4 = "1234FFFF00000000000000000000000000000000";
        String nodeIdFull5 = "1233FFFF00000000000000000000000000000000";
        nodeIds.add(nodeIdFull4);
        nodeIds.add(nodeIdFull5);

        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.AVAILABILITY_ZONE);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull4));
    }

    @Test
    public void testThatClosestIsCircularInAvzScope() throws Exception {
        idToCheck = rice.pastry.Id.build("1234DF");
        String nodeIdFull4 = "123405FF00000000000000000000000000000000";
        String nodeIdFull5 = "1235000000000000000000000000000000000000";
        nodeIds.add(nodeIdFull4);
        nodeIds.add(nodeIdFull5);

        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.AVAILABILITY_ZONE);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull4));
    }

    @Test
    public void testThatClosestReturnsNullIfNoNodesInRegionScope() throws Exception {
        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.REGION);

        // assert
        assertNull(result);
    }

    @Test
    public void testThatClosestChecksForClosestInRegionScope() throws Exception {
        String nodeIdFull4 = "12FFFFFF00000000000000000000000000000000";
        String nodeIdFull5 = "11FFFFFF00000000000000000000000000000000";
        nodeIds.add(nodeIdFull4);
        nodeIds.add(nodeIdFull5);

        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.REGION);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull4));
    }

    @Test
    public void testThatClosestIsCircularInRegionScope() throws Exception {
        idToCheck = rice.pastry.Id.build("12DF");
        String nodeIdFull4 = "1205FF0000000000000000000000000000000000";
        String nodeIdFull5 = "133500000000000000000000000000000000000000";
        nodeIds.add(nodeIdFull4);
        nodeIds.add(nodeIdFull5);

        // act
        Id result = koalaIdUtils.getNodeIdClosestToId(nodeIds, idToCheck, NodeScope.REGION);

        // assert
        assertThat(result.toStringFull(), equalTo(nodeIdFull4));
    }
}

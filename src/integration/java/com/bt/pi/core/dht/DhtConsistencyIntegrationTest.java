package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Id.Distance;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.past.DhtIntegrationTestBase;
import com.bt.pi.core.past.KoalaGCPastImpl;
import com.bt.pi.core.scope.NodeScope;

public class DhtConsistencyIntegrationTest extends DhtIntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(DhtConsistencyIntegrationTest.class);

    @Before
    public void before() throws Exception {
        numberOfNodes = 10;
        numberOfDhtRecordReplicas = 3;
        super.before();
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Override
    protected void seedInitialDhtRecords() {
    }

    @Test
    @Ignore
    public void shouldHaveReplicasOnOtherNodesAfterShutdown() throws Exception {
        // start with zeroth node
        changeContextToNode(0);
        PId initialNodeId = koalaNode.getKoalaIdFactory().convertToPId(koalaNode.getPastryNode().getId());
        int furthestNodeIndex = getFurthestNodeIndexFromId(initialNodeId);
        System.err.println("furthest node id = " + furthestNodeIndex);

        // bring down some adjacent nodes
        Map<Integer, Id> nodesBroughtDown = bringDownNodeAndNeighbours(0, 5);

        Thread.sleep(120000);

        // create a record in the area of the ring taken out
        changeContextToNode(furthestNodeIndex);
        printLeafsetForNode(furthestNodeIndex);
        MyDhtPiEntity myPiEntity = new MyDhtPiEntity("red");
        PId dhtId = new PiId(initialNodeId.toStringFull(), PId.getGlobalAvailabilityZoneCodeFromId(initialNodeId.toStringFull()));
        currentDhtClientFactory.createBlockingWriter().put(dhtId, myPiEntity);

        // check that the nearest nodes have the record in local storage, and that the others dont
        verifyLocalReplicaPlacement(initialNodeId);

        // bring up the missing nodes
        System.err.println("restoring nodes...");
        restoreNodes(nodesBroughtDown, furthestNodeIndex);

        Thread.sleep(60000);

        // make sure they have the record
        changeContextToNode(0);
        printLeafsetForNode(0);
        verifyLocalReplicaPlacement(initialNodeId);

        // make sure the other nodes don't
    }

    @Test
    @Ignore
    public void shouldReplicateBasedOnVersion() throws Exception {
        // write some data
        changeContextToNode(0);

        PId initialNodeId = koalaNode.getKoalaIdFactory().convertToPId(koalaNode.getPastryNode().getId());
        System.err.println("id = " + initialNodeId.toStringFull());

        int furthestNodeIndex = getFurthestNodeIndexFromId(initialNodeId);
        System.err.println("furthest node index = " + furthestNodeIndex);

        PId dataId = initialNodeId.forLocalAvailabilityZone();
        System.err.println("dataId = " + dataId.toStringFull());

        MyDhtPiEntity myPiEntity = new MyDhtPiEntity("green");
        currentDhtClientFactory.createBlockingWriter().put(dataId, myPiEntity);
        Thread.sleep(10000);
        verifyLocalReplicaPlacement(dataId, "green");

        // bring down some adjacent nodes#
        Map<Integer, Id> nodesBroughtDown = bringDownNodeAndNeighbours(0, 2);
        for (Id id : nodesBroughtDown.values())
            System.err.println(String.format("node %s is now down", id.toStringFull()));
        Thread.sleep(90000);
        // printLeafsetForNode(0);
        verifyLocalReplicaPlacement(dataId, false);

        // update data using part of ring that is still alive
        changeContextToNode(furthestNodeIndex);
        currentDhtClientFactory.createBlockingWriter().update(dataId, null, new UpdateResolver<MyDhtPiEntity>() {
            @Override
            public MyDhtPiEntity update(MyDhtPiEntity existingEntity, MyDhtPiEntity requestedEntity) {
                System.err.println(String.format("update(%s, %s)", existingEntity, requestedEntity));
                existingEntity.setName("orange");
                return existingEntity;
            }
        });
        System.err.println("updated to orange");
        LOG.info("updated to orange");
        Thread.sleep(90000);
        verifyLocalReplicaPlacement(dataId, false);

        // bring up the missing nodes
        LOG.info("bringing nodes up");
        restoreNodes(nodesBroughtDown, furthestNodeIndex);
        Thread.sleep(90000);
        changeContextToNode(0);

        // verify that versions in all 3 responsible nodes are up to date
        verifyLocalReplicaPlacement(dataId, "orange");

        // bring down daddy
        LOG.info("bringing daddy down");
        nodesBroughtDown = bringDownNodeAndNeighbours(0, 1);
        for (Id id : nodesBroughtDown.values())
            System.err.println(String.format("node %s is now down", id.toStringFull()));
        LOG.info("daddy is down");
        Thread.sleep(90000);
        verifyLocalReplicaPlacement(dataId, false);

        // now read data
        changeContextToNode(furthestNodeIndex);
        String result = currentDhtClientFactory.createBlockingReader().get(dataId).toString();
        assertEquals("orange", result);
    }

    private void verifyLocalReplicaPlacement(PId recordId) {
        verifyLocalReplicaPlacement(recordId, null, true);
    }

    private void verifyLocalReplicaPlacement(PId recordId, boolean doAssert) {
        verifyLocalReplicaPlacement(recordId, null, doAssert);
    }

    private void verifyLocalReplicaPlacement(PId recordId, String expectedContent) {
        verifyLocalReplicaPlacement(recordId, expectedContent, true);
    }

    private void verifyLocalReplicaPlacement(PId recordId, String expectedContent, boolean doAssert) {
        SortedMap<Distance, Integer> nodesSortedByDistance = sortByDistanceFrom(recordId);
        SortedSet<String> backupIds = ((KoalaGCPastImpl) currentPast).generateBackupIds(KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, NodeScope.REGION, recordId);
        int nodeCount = 0;
        boolean result = true;
        System.err.print(String.format("%-54s %-40s", " ", recordId.toStringFull()));
        for (String backupId : backupIds) {
            Id pastryBackupId = currentKoalaIdFactory.buildIdFromToString(backupId);
            System.err.print(String.format(" %40s", pastryBackupId.toStringFull()));
        }
        System.err.println();
        for (Entry<Distance, Integer> entry : nodesSortedByDistance.entrySet()) {
            changeContextToNode(entry.getValue());
            String nodeId = currentPastryNode.getId().toStringFull();
            MyDhtPiEntity readObject = readObjectFromLocalStorage(recordId, MyDhtPiEntity.class);
            System.err.print(String.format(" for node %s(%s):%s", entry.getValue(), nodeId, printEntity(readObject)));
            for (String backupId : backupIds) {
                PId pastryBackupId = currentKoalaIdFactory.buildPIdFromHexString(backupId);
                MyDhtPiEntity backupObject = readObjectFromLocalStorage(pastryBackupId, MyDhtPiEntity.class);
                System.err.print(printEntity(backupObject));
            }
            System.err.println();
            if (nodeCount <= numberOfDhtRecordReplicas) {
                if (null == readObject) {
                    result = false;
                } else {
                    if (null != expectedContent && doAssert)
                        if (!expectedContent.equals(readObject.getName()))
                            result = false;
                }
            } else {
                if (null != readObject)
                    result = false;
            }
            nodeCount++;
        }
        if (doAssert) {
            assertTrue(result);
        }
    }

    private String printEntity(MyDhtPiEntity myPiEntity) {
        if (null == myPiEntity) {
            return String.format(" %-40s", "null");
        }
        return String.format(" %-40s", String.format("%s(%d)", myPiEntity.toString(), myPiEntity.getVersion()));
    }
}

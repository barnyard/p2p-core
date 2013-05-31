package com.bt.pi.core.past;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Id.Distance;
import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.past.content.KoalaPiEntityContent;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingCreator;
import com.bt.pi.core.testing.RingHelper;

public abstract class DhtIntegrationTestBase extends RingHelper {
    private static final Log LOG = LogFactory.getLog(DhtIntegrationTestBase.class);
    protected static int numberOfNodes;
    protected static int numberOfDhtRecordReplicas = 4;
    protected static Properties properties;
    protected static final String DHT_CLIENT_FACTORY = "dhtClientFactory";
    protected static final String KOALA_ID_FACTORY = "koalaIdFactory";
    protected KoalaIdFactory currentKoalaIdFactory;
    protected DhtClientFactory currentDhtClientFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        deleteStorageDirectories();
        removeNodeIdFiles();
    }

    private static void deleteStorageDirectories() throws Exception {
        File root = new File(".");
        String[] list = root.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("storage");
            }
        });
        for (String s : list) {
            File d = new File(s);
            if (d.isDirectory())
                FileUtils.deleteQuietly(d);
        }
    }

    private static void removeNodeIdFiles() {
        String[] list = new File(".").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("nodeIdFile");
            }
        });
        for (String name : list)
            new File(name).delete();
    }

    @Before
    public void before() throws Exception {
        BeanPropertiesMunger.setDoMunging(true);
        super.before(numberOfNodes, null);

        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);
            getUtilityMapForNodeNumber(i).put(DHT_CLIENT_FACTORY, currentDhtClientFactory);
            getUtilityMapForNodeNumber(i).put(KOALA_ID_FACTORY, currentKoalaIdFactory);
        }

        seedInitialDhtRecords();
        BeanPropertiesMunger.setDoMunging(false);
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    protected abstract void seedInitialDhtRecords();

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        currentDhtClientFactory = (DhtClientFactory) currentApplicationContext.getBean(DHT_CLIENT_FACTORY);
        currentKoalaIdFactory = (KoalaIdFactory) currentApplicationContext.getBean(KOALA_ID_FACTORY);
    }

    protected void bringUpNodes(int numNodes, boolean shouldStartNewRing) throws Exception {
        SortedMap<Integer, Id> nodesToBringUp = new TreeMap<Integer, Id>();
        for (int i = 0; i < numNodes; i++) {
            Id nodeId = currentKoalaIdFactory.generateNodeId();
            nodesToBringUp.put(i, nodeId);
        }
        restoreNodes(nodesToBringUp, shouldStartNewRing ? 0 : null);
    }

    protected void restoreNodes(Map<Integer, Id> nodesAndIds, Integer bootstrapFromNode) throws Exception {
        changeContextToNode(0);
        String bootstrapList = "127.0.0.1:";
        if (bootstrapFromNode == null)
            bootstrapList = bootstrapList + (koalaNode.getPort() + numberOfNodes);
        else
            bootstrapList = bootstrapList + (koalaNode.getPort() + bootstrapFromNode);

        for (Entry<Integer, Id> entry : nodesAndIds.entrySet()) {
            BeanPropertiesMunger.setDoMunging(true);

            changeContextToNode(entry.getKey());
            System.err.println(String.format("restarting node %d - %s", entry.getKey(), entry.getValue().toStringFull()));
            RingCreator.createANode(entry.getKey());
            changeContextToNode(entry.getKey());
            koalaNode.setCanStartNewRing(false);
            koalaNode.setPreferredBootstraps(bootstrapList);
            koalaNode.start();
            waitForNode(koalaNode);
            Map<String, Object> utilityMap = getUtilityMapForNodeNumber(entry.getKey());
            KoalaGCPastImpl past = koalaNode.getPast();
            utilityMap.put(PAST, past);
            utilityMap.put(KOALA_DHT_STORAGE, past);
            utilityMap.put(STORAGE, past.getStorageManager());
            nodeApplicationsMap.put(entry.getKey(), utilityMap);

            BeanPropertiesMunger.setDoMunging(false);
        }
    }

    protected void waitForNode(KoalaNode aKoalaNode) throws Exception {
        int count = 0;
        while (aKoalaNode == null || aKoalaNode.getPastryNode() == null || (!aKoalaNode.getPastryNode().isReady() && count < 500)) {
            count++;
            System.err.print(".");
            Thread.sleep(150);
        }
        System.err.println("node " + aKoalaNode.getPastryNode().getId().toStringFull() + " is ready after count of " + count);
    }

    protected void takeNodesDown(int numNodes) throws Exception {
        for (int i = 0; i < numNodes; i++) {
            changeContextToNode(i);
            currentApplicationContext.destroy();
        }

        Thread.sleep(5000);
    }

    protected Map<Integer, Id> bringDownNodeAndNeighbours(int i, int numNeighboursToBringDown) {
        SortedMap<Distance, Integer> sortedNodes = sortByDistanceFrom(i);
        SortedMap<Integer, Id> nodesBroughtDown = new TreeMap<Integer, Id>();

        for (Entry<Distance, Integer> entry : sortedNodes.entrySet()) {
            if (nodesBroughtDown.size() >= numNeighboursToBringDown)
                break;

            System.err.println(String.format("Bringing down node %s with key %s", entry.getValue(), lordOfTheRings.getNode(entry.getValue()).getPastryNode().getId().toStringFull()));
            changeContextToNode(entry.getValue());
            nodesBroughtDown.put(entry.getValue(), koalaNode.getPastryNode().getId());
            currentApplicationContext.destroy();
        }
        return nodesBroughtDown;
    }

    protected void printLeafsetForNode(int i) {
        changeContextToNode(i);
        Id id = koalaNode.getPastryNode().getId();
        System.err.println("printing leafset for " + id.toStringFull());
        for (NodeHandle nh : koalaNode.getLeafNodeHandles())
            System.err.println("node handle: " + nh.getId().toStringFull());
    }

    protected int getNearestNodeIndexToId(Id id) {
        SortedMap<Distance, Integer> nodesSortedByDistance = sortByDistanceFrom(id);
        return nodesSortedByDistance.get(nodesSortedByDistance.firstKey());
    }

    protected int getFurthestNodeIndexFromId(PId pid) {
        Id id = rice.pastry.Id.build(pid.getIdAsHex());
        return getFurthestNodeIndexFromId(id);
    }

    protected int getFurthestNodeIndexFromId(Id id) {
        SortedMap<Distance, Integer> nodesSortedByDistance = sortByDistanceFrom(id);
        return nodesSortedByDistance.get(nodesSortedByDistance.lastKey());
    }

    protected SortedMap<Distance, Integer> sortByDistanceFrom(int i) {
        Id id = lordOfTheRings.getNode(i).getPastryNode().getId();
        return sortByDistanceFrom(id);
    }

    protected SortedMap<Distance, Integer> sortByDistanceFrom(PId pid) {
        Id id = rice.pastry.Id.build(pid.getIdAsHex());
        return sortByDistanceFrom(id);
    }

    protected SortedMap<Distance, Integer> sortByDistanceFrom(Id id) {
        SortedMap<Distance, Integer> nodesByDistance = new TreeMap<Distance, Integer>();
        for (int j = 0; j < numberOfNodes; j++) {
            if (lordOfTheRings.getNodes().get(j).getPastryNode() == null) {
                // ignoring dead node
                continue;
            }
            Distance distance = id.distanceFromId(lordOfTheRings.getNodes().get(j).getPastryNode().getNodeId());
            nodesByDistance.put(distance, j);
        }
        return nodesByDistance;
    }

    protected <T extends PiEntity> T readObjectFromLocalStorage(PId pid, final Class<T> clazz) {
        LOG.debug(String.format("readObjectFromLocalStorage(%s, %s) - %s", pid.toStringFull(), clazz.getName(), currentPastryNode.getId().toStringFull()));
        final Vector<T> res = new Vector<T>();
        final CountDownLatch latch = new CountDownLatch(1);
        // Id id = rice.pastry.Id.build(pid.forDht().getIdAsHex());
        Id id = rice.pastry.Id.build(pid.getIdAsHex());
        ((KoalaGCPastImpl) currentStorage).getStorageManager().getStorage().getObject(id, new GenericContinuation<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleResult(Object result) {
                LOG.debug(String.format("handleResult(%s)", result));
                if (result == null)
                    return;

                if (!(result instanceof KoalaPiEntityContent))
                    throw new RuntimeException("Expected KoalaPiEntityContent but was " + result);

                String serializedPiEntity = ((KoalaPiEntityContent) result).getBody();
                T t = (T) koalaJsonParser.getObject(serializedPiEntity, clazz);
                res.add(t);
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return res == null || res.isEmpty() ? null : res.get(0);
    }

    @Backupable
    @EntityScope(scope = NodeScope.REGION)
    public static class MyDhtPiEntity extends PiEntityBase {
        public static final int NUMBER_OF_BACKUPS = 2;
        private String name;
        private static final String SCHEME = "my";

        public MyDhtPiEntity() {
        }

        public MyDhtPiEntity(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return MyDhtPiEntity.class.getSimpleName();
        }

        @Override
        public String getUrl() {
            return String.format("%s:%s", SCHEME, name == null ? "pi" : name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public String getUriScheme() {
            return SCHEME;
        }
    }
}

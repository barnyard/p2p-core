package com.bt.pi.core.testing;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractApplicationContext;

import rice.p2p.past.Past;
import rice.pastry.PastryNode;
import rice.persistence.Storage;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.scribe.KoalaScribeImpl;

public abstract class RingHelper {
    protected static final String STORAGE = "storage";
    protected static final String PAST = "past";
    protected static final String KOALA_DHT_STORAGE = "koala-dht-storage";
    protected static final String P2P_INTEGRATION_PROPERTIES = "p2p.integration.properties";
    private static final Log LOG = LogFactory.getLog(RingHelper.class);

    public KoalaScribeImpl currentKoalaScribeImpl;
    public Past currentPast;
    public KoalaDHTStorage currentStorage;
    public PastryNode currentPastryNode;
    public Map<String, Object> currentAppMap;

    public KoalaNode koalaNode;
    protected static RingCreator lordOfTheRings;
    protected Storage currentPersistentStorage;
    protected AbstractApplicationContext currentApplicationContext;
    protected KoalaJsonParser koalaJsonParser;
    protected static Map<Integer, Map<String, Object>> nodeApplicationsMap;
    protected KoalaPiEntityFactory piEntityFactory;
    protected int region = 99;
    protected int zone = 66;
    protected String contextFile = RingCreator.DEFAULT_CONTEXT_FILE;
    private static boolean beforeMethodWasExecuted = false;

    public void beforeOnlyOnce(int numberOfNodes, Properties p) throws Exception {
        if (!beforeMethodWasExecuted) {
            before(numberOfNodes, p);
        } else
            changeContextToNode(0);
    }

    public void before(int numberOfNodes, Properties p) throws Exception {
        // reset currents
        org.springframework.util.FileSystemUtils.deleteRecursively(new File("./testStorage"));
        for (int i = 0; i < numberOfNodes; i++)
            FileUtils.deleteQuietly(new File(RingCreator.NODE_ID_FILE + i));
        Thread.sleep(5 * 1000);
        currentKoalaScribeImpl = null;
        currentPast = null;
        currentPastryNode = null;
        currentPersistentStorage = null;

        // create a ring!
        lordOfTheRings = new RingCreator(numberOfNodes, region, zone, p, contextFile);
        // create the ring without any apps.

        nodeApplicationsMap = new HashMap<Integer, Map<String, Object>>();
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            createApplicationsMapForNode(i);
        }
        changeContextToNode(0);
        beforeMethodWasExecuted = true;
    }

    public void createAdditionalNodes(int region, int zone, int numberOfNodes) {
        if (lordOfTheRings == null)
            return;
        int firstNodeCreated = lordOfTheRings.getRingSize();
        lordOfTheRings.setRegion(region);
        lordOfTheRings.setAvailabilityZone(zone);
        lordOfTheRings.createNodes(numberOfNodes);
        for (int i = firstNodeCreated; i < lordOfTheRings.getRingSize(); i++) {
            createApplicationsMapForNode(i);
        }
    }

    private void createApplicationsMapForNode(int nodeIndex) {
        Map<String, Object> utilityMap = new HashMap<String, Object>();

        utilityMap.put("scribe", lordOfTheRings.getNode(nodeIndex).getScribe());
        Past past = lordOfTheRings.getNode(nodeIndex).getPast();
        utilityMap.put(PAST, past);
        utilityMap.put(KOALA_DHT_STORAGE, past);
        utilityMap.put(STORAGE, lordOfTheRings.getNode(nodeIndex).getPersistentDhtStorage());
        nodeApplicationsMap.put(nodeIndex, utilityMap);
    }

    public void after() throws Exception {
        afterClass();
    }

    public static void afterClass() throws Exception {
        // destroy the scribe apps associated with the node.
        LOG.info(String.format("Destroying TEST ring"));
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            File file = new File(RingCreator.NODE_ID_FILE + i);
            if (file.exists())
                file.delete();
        }

        lordOfTheRings.destroy();
        lordOfTheRings = null;
        nodeApplicationsMap = null;
    }

    protected Map<String, Object> getUtilityMapForNodeNumber(int nodeNumber) {
        Map<String, Object> utilityMap;
        if (nodeApplicationsMap.containsKey(nodeNumber)) {
            utilityMap = nodeApplicationsMap.get(nodeNumber);
        } else
            utilityMap = new HashMap<String, Object>();
        return utilityMap;
    }

    public void setDataTypes(List<PiEntity> dataTypes) {
        piEntityFactory.setPiEntityTypes(dataTypes);
    }

    public void changeContextToNode(int nodeNumber) {
        LOG.debug(String.format("changeContextToNode(%d)", nodeNumber));
        currentAppMap = nodeApplicationsMap.get(nodeNumber);

        currentKoalaScribeImpl = ((KoalaScribeImpl) currentAppMap.get("scribe"));
        currentPast = ((Past) currentAppMap.get(PAST));
        koalaNode = lordOfTheRings.getNode(nodeNumber);
        currentPastryNode = koalaNode.getPastryNode();
        currentStorage = (KoalaDHTStorage) currentAppMap.get(KOALA_DHT_STORAGE);
        currentPersistentStorage = (Storage) currentAppMap.get(STORAGE);
        currentApplicationContext = (AbstractApplicationContext) lordOfTheRings.getApplicationContextForNode(nodeNumber);
        piEntityFactory = currentApplicationContext.getBean(KoalaPiEntityFactory.class);
        koalaJsonParser = currentApplicationContext.getBean(KoalaJsonParser.class);
        updateLocalsForNodeContextChange(currentAppMap);
    }

    public abstract void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap);
}
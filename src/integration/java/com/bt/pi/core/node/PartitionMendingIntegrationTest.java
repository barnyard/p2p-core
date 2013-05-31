package com.bt.pi.core.node;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class PartitionMendingIntegrationTest extends RingHelper {
    private static Properties properties;
    private static final String KOALA_ID_FACTORY = "koalaIdFactory";
    private int numberOfNodes = 4;

    private int nodesToTakeDown = 2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(KoalaNodeIntegrationTest.class.getClassLoader().getResourceAsStream("p2p.integration.properties"));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void testBefore() throws Exception {
        before(numberOfNodes, properties);

        for (int i = 0; i < numberOfNodes; i++) {
            nodeApplicationsMap.put(i, getUtilityMapForNodeNumber(i));
            getUtilityMapForNodeNumber(i).put(KOALA_ID_FACTORY, lordOfTheRings.getNode(i).getKoalaIdFactory());
        }
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
    }

    private void takeDownNodesAndBringUpInAnotherRing(int numNodes, boolean shouldStartNewRing) throws Exception {
        changeContextToNode(0);
        String bootstrapList = InetAddress.getLocalHost().getHostAddress() + ":" + (koalaNode.getPort() + (shouldStartNewRing ? 0 : numNodes));

        lordOfTheRings.destroy();

        Thread.sleep(20 * 1000);
        lordOfTheRings.setBootstrapList(bootstrapList);
        lordOfTheRings.createNodes(numberOfNodes);
    }

    /*
     *      properties used so you won't have to wait all day.    
     *      
     *      p.setProperty("pastry_socket_allow_loopback", "true");
            p.setProperty("pastry_lSetSize", "10");
            p.setProperty("pastry_factory_selectorPerNode", "true");
            p.setProperty("pastry_factory_processorPerNode", "true");
            p.setProperty("pastry_leafSetMaintFreq", "10");
            p.setProperty("pastry_leafSetMaintFreq", "10");
            p.setProperty("pastry_protocol_router_routeMsgVersion", "0");

            // testing different pastry parameters
            p.setProperty("pastry_socket_writer_max_queue_length", "100");

            p.setProperty("pastry_socket_srm_check_dead_throttle", "30000");
            p.setProperty("pastry_socket_srm_proximity_timeout", "36000");
            p.setProperty("pastry_socket_srm_ping_throttle", "3000");

            p.setProperty("pastry_socket_scm_num_ping_tries", "20");
            p.setProperty("pastry_socket_scm_write_wait_time", "10000");
            p.setProperty("pastry_socket_scm_backoff_initial", "200");
            p.setProperty("pastry_socket_scm_backoff_limit", "20");

            p.setProperty("pastry_protocol_consistentJoin_max_time_to_be_scheduled", "1500");
            p.setProperty("pastry_protocol_consistentJoin_retry_interval", "3000");
            p.setProperty("pastry_protocol_consistentJoin_failedRetentionTime", "9999000");
            p.setProperty("pastry_protocol_consistentJoin_cleanup_interval", "9999000");
            p.setProperty("pastry_protocol_consistentJoin_maxFailedToSend", "100");
            p.setProperty("pastry_protocol_periodicLeafSet_ping_neighbor_period", "2000");
            p.setProperty("pastry_protocol_periodicLeafSet_lease_period", "3000");
            p.setProperty("pastry_protocol_periodicLeafSet_request_lease_throttle", "1000");

            p.setProperty("pastry_factory_selectorPerNode", "true");
            p.setProperty("pastry_factory_processorPerNode", "true");

     */
    // @Ignore
    @Test
    public void testLeafSetReforms() throws Exception {
        // act

        // First we take down the nodes. and then bring them back up.
        takeDownNodesAndBringUpInAnotherRing(nodesToTakeDown, true);
        Thread.sleep(30 * 1000);

        // Then we wait and check the leafsets to see if the ring has been completed.
        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);
            System.err.println(String.format("Node %s: leafesetSize: %s leafset: %s", koalaNode.getPastryNode().getId(), koalaNode.getLeafNodeHandles().size(), koalaNode.getLeafNodeHandles()));
        }

        Thread.sleep(30 * 1000);

        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);
            System.err.println(String.format("Node %s: leafesetSize: %s leafset: %s", koalaNode.getPastryNode().getId(), koalaNode.getLeafNodeHandles().size(), koalaNode.getLeafNodeHandles()));
        }

        // Thread.sleep(240 * 1000);
        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);
            System.err.println(String.format("Node %s: leafesetSize: %s leafset: %s", koalaNode.getPastryNode().getId(), koalaNode.getLeafNodeHandles().size(), koalaNode.getLeafNodeHandles()));
        }
    }
}

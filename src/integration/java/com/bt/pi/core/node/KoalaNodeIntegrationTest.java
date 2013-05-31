package com.bt.pi.core.node;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class KoalaNodeIntegrationTest extends RingHelper {
    private static Properties properties;
    private int numberOfNodes = 5;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(KoalaNodeIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void testBefore() throws Exception {
        super.before(numberOfNodes, properties);
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        // TODO Auto-generated method stub
    }

    @Test
    public void nodesShouldAllHaveDifferentIdsButShareRegionAndZone() {
        // assert
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            KoalaNode node = lordOfTheRings.getNode(i);
            String nodeId = node.getPastryNode().getNodeId().toStringFull();
            System.out.println(String.format("node %d: %s", i, nodeId));
            ids.add(nodeId);
        }
        assertEquals(numberOfNodes, ids.size());
        for (String id : ids) {
            assertEquals(region, Integer.parseInt(id.substring(0, 2), 16));
            assertEquals(zone, Integer.parseInt(id.substring(2, 4), 16));
        }
    }
}

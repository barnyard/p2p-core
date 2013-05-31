package com.bt.pi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.application.EchoApplication;
import com.bt.pi.core.application.InstrumentedEchoApplication;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class NodeDepartureDetectionIntegrationTest extends RingHelper {
    private int numberOfNodes;

    private static Properties properties;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(NodeDepartureDetectionIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void before() throws Exception {
        numberOfNodes = 15;

        super.before(numberOfNodes, properties);

        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);

            // piEntityFactory = currentApplicationContext.getBean(KoalaPiEntityFactory.class);

            // ArrayList<PiEntity> applicationPayloads = new ArrayList<PiEntity>();
            // applicationPayloads.add(new EchoPayload());

            // piEntityFactory.setPiEntityTypes(applicationPayloads);
            // piEntityFactory.setPiEntityTypes(applicationPayloads);

            // List<PiEntity> piEntities = Arrays.asList(new PiEntity[] { new EchoPayload() });

            // piEntityFactory.setPiEntityTypes(piEntities);

            // List<KoalaMessageBase> messages = Arrays.asList(new KoalaMessageBase[] { new ApplicationMessage(), new
            // EchoMessage() });

            // KoalaMessageFactoryImpl koalaMessageFactory =
            // currentApplicationContext.getBean(KoalaMessageFactoryImpl.class);
            // koalaMessageFactory.setApplicationMessagePayloadTypes(piEntities);
            // koalaMessageFactory.setApplicationMessageTypes(messages);

            ContinuationRequestWrapperImpl continuationWrapper = mock(ContinuationRequestWrapperImpl.class);
            when(continuationWrapper.messageReceived(isA(PId.class), isA(KoalaMessage.class))).thenReturn(true);

            EchoApplication echoApplication = currentApplicationContext.getBean(InstrumentedEchoApplication.class);
            // echoApplication.setKoalaPiEntityFactory(piEntityFactory);

            echoApplication.setContinuationRequestWrapper(continuationWrapper);

            Map<String, Object> applicationsMap = new HashMap<String, Object>();
            applicationsMap.put(echoApplication.getApplicationName(), echoApplication);

            nodeApplicationsMap.put(i, applicationsMap);
        }
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Test
    public void shouldOnlyReceiveOneNodeDepartureNotification() throws Exception {
        // Setup
        Random rand = new Random();

        // act destroy a node
        int nodeNum = rand.nextInt(numberOfNodes);
        changeContextToNode(nodeNum);
        String destroyedNodeId = currentPastryNode.getId().toStringFull();
        Thread.sleep(10 * 1000);

        // act
        System.err.println("Destroying node " + nodeNum + " with id: " + destroyedNodeId);
        koalaNode.stop();

        Thread.sleep(60 * 1000);

        // assert
        assertEquals(1, InstrumentedEchoApplication.detectionCount);
        assertTrue(InstrumentedEchoApplication.departedNodesHash.containsKey(destroyedNodeId));
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {

    }
}

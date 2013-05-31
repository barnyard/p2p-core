package com.bt.pi.core.application.health;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;

import rice.Continuation;

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.node.KoalaNodeIntegrationTest;
import com.bt.pi.core.testing.RingHelper;

public abstract class HealthApplicationIntegrationTestBase extends RingHelper {
    protected static Properties properties;

    private static final Log LOG = LogFactory.getLog(HealthApplicationIntegrationTestBase.class);
    private int numberOfNodes = 5;

    protected ScheduledExecutorService scheduledExecutor;
    protected SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;
    private PId superNodeApplicationCheckPointsId;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(KoalaNodeIntegrationTest.class.getClassLoader().getResourceAsStream("p2p.integration.properties"));

    }

    @Before
    public void setup() throws Exception {
        LOG.info("Create ring with " + numberOfNodes + " nodes");
        super.before(numberOfNodes, properties);
        printNodeLeafsetSizes();

        scheduledExecutor = Executors.newScheduledThreadPool(20);

        lordOfTheRings.getNode(0).getKoalaIdFactory().setKoalaPiEntityFactory(piEntityFactory);

        DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);

        superNodeApplicationCheckPoints = new SuperNodeApplicationCheckPoints();
        superNodeApplicationCheckPointsId = lordOfTheRings.getNode(0).getKoalaIdFactory().buildPId(SuperNodeApplicationCheckPoints.URL);

        int numberOfSuperNodes = 2;
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication(ReportingApplication.APPLICATION_NAME, numberOfSuperNodes, 1);
        LOG.debug("Seeding SuperNodes");
        dhtClientFactory.createBlockingWriter().writeIfAbsent(superNodeApplicationCheckPointsId, superNodeApplicationCheckPoints);
        LOG.debug("Super Nodes seeded");
    }

    private void printNodeLeafsetSizes() {
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            ApplicationContext applicationContext = lordOfTheRings.getApplicationContextForNode(i);
            System.err.println(String.format("Leafset size for node %s: %d", ((KoalaNode) applicationContext.getBean("koalaNode")).getPastryNode().getNodeId().toStringFull(), ((KoalaNode) applicationContext.getBean("koalaNode")).getLeafNodeHandles()
                    .size()));
        }
    }

    @After
    public void cleanUp() throws Exception {
        scheduledExecutor.shutdown();
        after();
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
    }

    @SuppressWarnings("unchecked")
    protected void querySupernodeApplication(PiEntity piEntity, Continuation continuation) throws Exception {
        ReportingApplication myApplication = getNonSupernodeAndNonCurrentApplicationContext().getBean(ReportingApplication.class);
        MessageContext messageContext = myApplication.newMessageContext();
        PId id = koalaNode.getKoalaIdFactory().buildPIdFromHexString(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, zone));
        messageContext.routePiMessageToApplication(id, EntityMethod.GET, piEntity, ReportingApplication.APPLICATION_NAME, continuation);
    }

    private ApplicationContext getNonSupernodeAndNonCurrentApplicationContext() {
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            ApplicationContext applicationContext = lordOfTheRings.getApplicationContextForNode(i);
            System.err.println(applicationContext + " " + applicationContext.getBean(ApplicationRegistry.class).getApplicationStatus(ReportingApplication.APPLICATION_NAME));
            if (applicationContext != currentApplicationContext && !applicationContext.getBean(ApplicationRegistry.class).getApplicationStatus(ReportingApplication.APPLICATION_NAME).equals(ApplicationStatus.ACTIVE))
                return applicationContext;
        }
        return null;
    }
}

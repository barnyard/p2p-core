package com.bt.pi.core.application;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.application.activation.ActivationAwareApplication;
import com.bt.pi.core.application.activation.ApplicationActivationCheckStatus;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.InterApplicationDependenciesStore;
import com.bt.pi.core.application.activation.SuperNodeApplicationActivator;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.node.KoalaNodeIntegrationTest;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class SuperNodeApplicationTest extends RingHelper {
    private static Properties properties;
    private int numberOfNodesPerAvailabilityZone = 5;
    private int numberOfAvailabilityZonesStarted;

    private ExecutorService executor;
    private SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;
    private PId superNodeApplicationCheckPointsId;
    private DhtClientFactory dhtClientFactory0;
    private CountDownLatch superNodeApplicationLatch;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(KoalaNodeIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void setup() throws Exception {
        super.before(numberOfNodesPerAvailabilityZone, properties);

        superNodeApplicationLatch = new CountDownLatch(numberOfNodesPerAvailabilityZone * 4);
        numberOfAvailabilityZonesStarted = 1;
        startNodesInRegionAndZone(region, zone + 1);
        startNodesInRegionAndZone(region + 1, zone);
        startNodesInRegionAndZone(region + 1, zone + 1);

        changeContextToNode(0);
        executor = Executors.newCachedThreadPool();
        dhtClientFactory0 = currentApplicationContext.getBean(DhtClientFactory.class);
        dhtClientFactory0.setKoalaDhtStorage(currentStorage);
        dhtClientFactory0.setExecutor(executor);

        superNodeApplicationCheckPoints = new SuperNodeApplicationCheckPoints();
        superNodeApplicationCheckPointsId = lordOfTheRings.getNode(0).getKoalaIdFactory().buildPId(SuperNodeApplicationCheckPoints.URL);
    }

    private void startNodesInRegionAndZone(int region, int zone) throws InterruptedException {
        createAdditionalNodes(region, zone, numberOfNodesPerAvailabilityZone);
        numberOfAvailabilityZonesStarted++;
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    private void startSuperNodeApplication() {
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);

            DhtClientFactory dhtClientFactory = currentApplicationContext.getBean(DhtClientFactory.class);
            dhtClientFactory.setKoalaDhtStorage(currentStorage);
            dhtClientFactory.setExecutor(executor);

            Cache cache = new Cache("test", 20, false, false, 300, 300);
            cache.bootstrap();
            cache.initialise();
            cache.put(new Element(superNodeApplicationCheckPointsId, superNodeApplicationCheckPoints));

            DhtCache dhtCache = new DhtCache();
            dhtCache.setDhtClientFactory(dhtClientFactory);
            dhtCache.setCache(cache);

            ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(20);

            SuperNodeApplicationActivator superNodeApplicationActivator = new SuperNodeApplicationActivator() {
                @Override
                public ApplicationActivationCheckStatus initiateActivationChecks(ActivationAwareApplication application) {
                    ApplicationActivationCheckStatus initiateActivationChecks = super.initiateActivationChecks(application);
                    superNodeApplicationLatch.countDown();
                    return initiateActivationChecks;
                }
            };
            superNodeApplicationActivator.setDhtCache(dhtCache);
            superNodeApplicationActivator.setKoalaIdFactory(koalaNode.getKoalaIdFactory());
            superNodeApplicationActivator.setKoalaIdUtils(new KoalaIdUtils());
            superNodeApplicationActivator.setExecutor(executor);
            superNodeApplicationActivator.setApplicationRegistry(new ApplicationRegistry());
            superNodeApplicationActivator.setScheduledExecutorService(scheduledExecutorService);
            superNodeApplicationActivator.setInterApplicationDependenciesStore(currentApplicationContext.getBean(InterApplicationDependenciesStore.class));

            MySuperNodeApplication superNodeApplication = new MySuperNodeApplication(superNodeApplicationActivator);
            Map<String, KoalaPastryApplicationBase> nodeApplications = new HashMap<String, KoalaPastryApplicationBase>();
            nodeApplications.put(MySuperNodeApplication.APPLICATION_NAME, superNodeApplication);
            superNodeApplication.start(currentPastryNode, currentStorage, nodeApplications, koalaNode.getPersistentDhtStorage());

            currentAppMap.put(MySuperNodeApplication.APPLICATION_NAME, superNodeApplication);
            System.err.println(String.format("Starting super node %d", i));
        }
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
    }

    @Test
    public void testNumberOfSuperNodesWithScopeAsAvailabilityZone() throws Exception {
        int numberOfSuperNodes = 2;
        superNodeApplicationCheckPoints.setSuperNodeCheckPointsForApplication(MySuperNodeApplication.APPLICATION_NAME, numberOfSuperNodes, 1);
        dhtClientFactory0.createBlockingWriter().writeIfAbsent(superNodeApplicationCheckPointsId, superNodeApplicationCheckPoints);
        System.err.println("Super Nodes seeded");

        startSuperNodeApplication();
        assertTrue("Super node apps did not start up in time", superNodeApplicationLatch.await(30, TimeUnit.SECONDS));

        int count = 0;
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);
            MySuperNodeApplication superNodeApplication = (MySuperNodeApplication) currentAppMap.get(MySuperNodeApplication.APPLICATION_NAME);
            if (superNodeApplication.isActive.get())
                count++;

            System.err.println(String.format("Node number %d (%s) is %s", i, koalaNode.getKoalaIdFactory().generateNodeId().toStringFull(), superNodeApplication.isActive.get() ? "ACTIVE" : "PASSIVE"));
        }
        System.err.println("Super Node checkpoints:");
        System.err.println(superNodeApplicationCheckPoints.getSuperNodeCheckPoints(MySuperNodeApplication.APPLICATION_NAME, region, zone));
        System.err.println(superNodeApplicationCheckPoints.getSuperNodeCheckPoints(MySuperNodeApplication.APPLICATION_NAME, region, zone + 1));
        System.err.println(superNodeApplicationCheckPoints.getSuperNodeCheckPoints(MySuperNodeApplication.APPLICATION_NAME, region + 1, zone));
        System.err.println(superNodeApplicationCheckPoints.getSuperNodeCheckPoints(MySuperNodeApplication.APPLICATION_NAME, region + 1, zone + 1));

        assertTrue(String.valueOf(count), count > 0 && count <= numberOfSuperNodes * numberOfAvailabilityZonesStarted);
    }

    public static class MySuperNodeApplication extends SuperNodeApplicationBase {
        public static final String APPLICATION_NAME = "supernode-test";
        private AtomicBoolean isActive = new AtomicBoolean(false);

        public MySuperNodeApplication(SuperNodeApplicationActivator superNodeApplicationActivator) {
            setApplicationActivator(superNodeApplicationActivator);
            setKoalaJsonParser(new KoalaJsonParser());
            setContinuationRequestWrapper(new ContinuationRequestWrapperImpl());
        }

        @Override
        public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data) {
        }

        @Override
        public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
            return false;
        }

        @Override
        public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        }

        @Override
        public void handleNodeDeparture(String nodeId) {
        }

        @Override
        public boolean becomeActive() {
            isActive.set(true);
            return true;
        }

        @Override
        public void becomePassive() {
            isActive.set(false);
        }

        @Override
        public String getApplicationName() {
            return APPLICATION_NAME;
        }

        @Override
        protected NodeScope getSuperNodeTopicScope() {
            return NodeScope.AVAILABILITY_ZONE;
        }

        @Override
        protected String getSuperNodeTopicUrl() {
            return "topic:test";
        }
    }
}

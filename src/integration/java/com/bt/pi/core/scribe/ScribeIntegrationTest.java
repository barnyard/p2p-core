package com.bt.pi.core.scribe;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterOrEqual;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.messaging.KoalaMessageDeserializer;
import com.bt.pi.core.messaging.KoalaMessageFactoryImpl;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class ScribeIntegrationTest extends RingHelper implements SubscribeDataReceivedListener {
    private static Properties properties;

    private PiLocation topic;
    private int numberOfNodes = 6;
    private Map<Integer, Integer> receivedNodeNumbers;
    private Collection<Object> receivedObjects;

    private KoalaScribeApplication currentKoalaScribeApplication;
    private CountDownLatch dataReceivedLatch;
    private CountDownLatch subscribeLatch;
    private CountDownLatch anycastFailureLatch;
    private KoalaMessageDeserializer koalaMessageDeserializer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(ScribeIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void testBefore() throws Exception {
        super.before(numberOfNodes, properties);

        receivedNodeNumbers = new HashMap<Integer, Integer>();
        receivedObjects = new ArrayList<Object>();

        List<PiEntity> piEntities = Arrays.asList(new PiEntity[] { new MyApplicationRecord(), new LogMessageEntityCollection() });

        topic = new PiLocation("ScribeIntegrationTest", NodeScope.AVAILABILITY_ZONE);

        KoalaMessageFactoryImpl koalaMessageFactory = new KoalaMessageFactoryImpl();
        koalaMessageFactory.setApplicationMessagePayloadTypes(piEntities);
        koalaMessageDeserializer = new KoalaMessageDeserializer();
        koalaMessageDeserializer.setKoalaMessageFactory(koalaMessageFactory);

        allNodesSubscribeToTopic();
        dataReceivedLatch = new CountDownLatch(6);
    }

    private void allNodesSubscribeToTopic() throws InterruptedException {
        subscribeLatch = new CountDownLatch(numberOfNodes);
        anycastFailureLatch = new CountDownLatch(1);

        for (int i = 0; i < numberOfNodes; i++) {
            changeContextToNode(i);

            currentKoalaScribeApplication = new KoalaScribeApplication(i, this, subscribeLatch, anycastFailureLatch);

            Map<String, KoalaPastryApplicationBase> nodeApplications = new HashMap<String, KoalaPastryApplicationBase>();
            nodeApplications.put(currentKoalaScribeApplication.getApplicationName(), currentKoalaScribeApplication);

            currentKoalaScribeApplication.setDeserializer(koalaMessageDeserializer);
            currentKoalaScribeApplication.setKoalaIdFactory(koalaNode.getKoalaIdFactory());
            currentKoalaScribeApplication.setKoalaPiEntityFactory(piEntityFactory);
            currentKoalaScribeApplication.setKoalaJsonParser(koalaJsonParser);
            currentKoalaScribeApplication.setScribe(currentKoalaScribeImpl);
            currentKoalaScribeApplication.start(currentPastryNode, currentStorage, nodeApplications, null);
            getUtilityMapForNodeNumber(i).put(currentKoalaScribeApplication.getApplicationName(), currentKoalaScribeApplication);

            currentKoalaScribeApplication.subscribe(topic, currentKoalaScribeApplication);
        }
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Test
    public void sendLargeMessage() throws Exception {
        // setup
        assertThat(subscribeLatch.await(10, TimeUnit.SECONDS), is(true));
        DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        final AtomicInteger dhtReadsCompleted = new AtomicInteger();
        final AtomicInteger timeTakenMillis = new AtomicInteger();
        final CountDownLatch sendCompletedLatch = new CountDownLatch(1);
        final CountDownLatch testCompletedLatch = new CountDownLatch(1);

        final ApplicationRecord rec = new GlobalScopedApplicationRecord("big mama");
        final List<String> resources = new ArrayList<String>();
        for (int i = 0; i < 10000; i++)
            resources.add(Long.toString(new Random().nextLong()));
        rec.addResources(resources);

        // act
        // so lets do some stuff in the selector thread.... lets spawn off a worker thread to keep trying to do some dht
        // reads, then without releasing the selector thread lets do a send of a big mama message
        dhtClientFactory.createReader().getAsync(new PiId(rice.pastry.Id.build("something else").toStringFull(), 0), new PiContinuation<PiEntity>() {
            @Override
            public void handleResult(PiEntity result) {
                keepReadingDhtRecordsInSeparateThread(dhtReadsCompleted, testCompletedLatch);

                final AtomicLong timeBefore = new AtomicLong(System.currentTimeMillis());
                new PubSubMessageContext(currentKoalaScribeApplication, topic, null).sendAnycast(EntityMethod.UPDATE, rec);

                timeTakenMillis.set((int) (System.currentTimeMillis() - timeBefore.get()));
                System.err.println("Send with getjson took " + timeTakenMillis + " millis");
                sendCompletedLatch.countDown();
            }
        });

        sendCompletedLatch.await();
        // assertEquals(0, dhtReadsCompleted.get());
        System.err.println("parallel dht reads done: " + dhtReadsCompleted.toString());
        assertTrue("Send should have taken (much) less than a minute", timeTakenMillis.get() < 60 * 1000);
    }

    @Test
    public void testPublish() throws Exception {
        // setup
        assertThat(subscribeLatch.await(10, TimeUnit.SECONDS), is(true));

        PiEntity piEntity1 = createLogMessageEntityCollection(1);
        PiEntity piEntity2 = createLogMessageEntityCollection(2);
        PiEntity piEntity3 = createLogMessageEntityCollection(3);
        PiEntity piEntity4 = createLogMessageEntityCollection(4);
        PiEntity piEntity5 = createLogMessageEntityCollection(5);
        PiEntity piEntity6 = createLogMessageEntityCollection(6);

        // act
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity1);
        Thread.sleep(1000);
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity2);
        Thread.sleep(1000);
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity3);
        Thread.sleep(1000);
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity4);
        Thread.sleep(1000);
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity5);
        Thread.sleep(1000);
        new PubSubMessageContext(currentKoalaScribeApplication, topic, null).publishContent(EntityMethod.UPDATE, piEntity6);

        // assert
        assertThat(dataReceivedLatch.await(40, TimeUnit.SECONDS), is(true));
        System.err.println("Received 6 objects");
        // assertThat(receivedObjects.contains(piEntity1), is(true));
        // assertThat(receivedObjects.contains(piEntity2), is(true));
        // assertThat(receivedObjects.contains(piEntity3), is(true));
        // assertThat(receivedObjects.contains(piEntity4), is(true));
        // assertThat(receivedObjects.contains(piEntity5), is(true));
        // assertThat(receivedObjects.contains(piEntity6), is(true));
    }

    // public static void main(String[] args) throws Exception {
    // ScribeIntegrationTest scribeIntegrationTest = new ScribeIntegrationTest();
    // ScribeIntegrationTest.beforeClass();
    // scribeIntegrationTest.testBefore();
    // scribeIntegrationTest.testPublish();
    // Thread.sleep(900000);
    // scribeIntegrationTest.after();
    // ScribeIntegrationTest.afterClass();
    // }

    private PiEntity createLogMessageEntityCollection(int index) {
        Collection<LogMessageEntity> ents = new ArrayList<LogMessageEntity>();
        long currentTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ents.add(new LogMessageEntity(currentTimeMillis, "index " + index + ": test " + 0 + ": how can me make this test fail. does the size of the collection really matter? what's going on here", "test", "", ""));
        }

        LogMessageEntityCollection logMessageEntityCollection = new LogMessageEntityCollection();
        logMessageEntityCollection.setEntities(ents);
        return logMessageEntityCollection;
    }

    private void keepReadingDhtRecordsInSeparateThread(final AtomicInteger dhtReadsCompleted, final CountDownLatch completedLatch) {
        final DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!completedLatch.await(100, TimeUnit.MILLISECONDS)) {
                        dhtClientFactory.createReader().getAsync(new PiId(rice.pastry.Id.build("something").toStringFull(), 0), new PiContinuation<PiEntity>() {
                            @Override
                            public void handleResult(PiEntity result) {
                                dhtReadsCompleted.incrementAndGet();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "bgnd-dht-read-worker").start();
    }

    @Test
    public void randomAnycastShouldBeJustThat() throws Exception {
        // setup
        assertThat(subscribeLatch.await(10, TimeUnit.SECONDS), is(true));
        int numberOfReAnycasts = 40;
        dataReceivedLatch = new CountDownLatch(numberOfReAnycasts);

        List<PiEntity> piEntities = new ArrayList<PiEntity>();

        // act
        for (int i = 0; i < numberOfReAnycasts; i++) {
            PiEntity piEntity = new MyApplicationRecord("MyApplicationRecord " + i);
            new PubSubMessageContext(currentKoalaScribeApplication, topic, null).randomAnycast(EntityMethod.UPDATE, piEntity);
            piEntities.add(piEntity);

        }

        // assert
        assertThat(dataReceivedLatch.await(400, TimeUnit.SECONDS), is(true));
        assertThat(receivedObjects.size(), equalTo(piEntities.size()));
        for (PiEntity piEntity : piEntities) {
            assertThat(receivedObjects.contains(piEntity), is(true));
        }
        System.err.println("NODE IDS:");
        for (KoalaNode koalaNode : lordOfTheRings.getNodes().values()) {
            System.err.println(koalaNode.getKoalaIdFactory().generateNodeId().toStringFull());
        }
        System.err.println("RECEIVED NODE NUMBERS");
        for (int nodeNumber : receivedNodeNumbers.keySet()) {
            System.err.println(lordOfTheRings.getNode(nodeNumber).getKoalaIdFactory().generateNodeId().toStringFull() + " count: " + receivedNodeNumbers.get(nodeNumber));
        }
        assertThat(receivedNodeNumbers.size(), new GreaterOrEqual<Integer>(numberOfNodes));

    }

    @Override
    public void dataReceived(Object data, int nodeNumber) {
        int nodeCount = receivedNodeNumbers.get(nodeNumber) == null ? 1 : receivedNodeNumbers.get(nodeNumber) + 1;
        receivedNodeNumbers.put(nodeNumber, nodeCount);
        if (data instanceof ApplicationRecord)
            receivedObjects.add(new MyApplicationRecord((ApplicationRecord) data));
        else if (!receivedObjects.contains(data))
            receivedObjects.add(data);
        dataReceivedLatch.countDown();
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        currentKoalaScribeApplication = (KoalaScribeApplication) currentApplicationMap.get("koala-test-app");
        DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        dhtClientFactory.setKoalaDhtStorage(currentStorage);
    }

    public static class MyApplicationRecord extends GlobalScopedApplicationRecord implements Comparable<MyApplicationRecord> {

        public MyApplicationRecord() {

        }

        public MyApplicationRecord(ApplicationRecord applicationRecord) {
            super(applicationRecord.getApplicationName());
        }

        public MyApplicationRecord(String applicationName) {
            super(applicationName);
        }

        @Override
        public String getType() {
            return this.getClass().getSimpleName();
        }

        @Override
        public int compareTo(MyApplicationRecord o) {
            return getApplicationName().compareTo(o.getApplicationName());
        }

        @Override
        public String getUriScheme() {
            return "My" + super.getUriScheme();
        }
    }
}

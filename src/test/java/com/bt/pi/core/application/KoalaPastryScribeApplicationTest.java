package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.Topic;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;

import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.testing.LogHelper;
import com.bt.pi.core.testing.VectorAppender;

public class KoalaPastryScribeApplicationTest {
    private static final String EXISTING_TX_UID = "existing-tx-uid";
    private KoalaPastryScribeApplicationBase koalaPastryScribeApplication;
    private Topic topic;
    private PastryNode node;
    private KoalaScribeImpl scribe;
    private NodeHandle nodeHandle;
    private KoalaPiEntityFactory koalaScribeDataParser;
    private String transactionUID;
    private KoalaIdFactory idFactory;
    private PId testEntityTopicPId;

    @Before
    public void before() {
        LogHelper.initLogging();

        nodeHandle = mock(NodeHandle.class);

        node = mock(PastryNode.class);
        Endpoint endpoint = mock(Endpoint.class);
        when(node.buildEndpoint(isA(KoalaPastryScribeApplicationBase.class), isA(String.class))).thenReturn(endpoint);
        when(node.getLocalNodeHandle()).thenReturn(nodeHandle);

        idFactory = new KoalaIdFactory(22, 22);
        idFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());

        testEntityTopicPId = idFactory.buildPId("bob").forLocalScope(NodeScope.REGION);
        topic = new Topic(idFactory.buildId(testEntityTopicPId));

        transactionUID = UUID.randomUUID().toString();

        scribe = mock(KoalaScribeImpl.class);
        when(scribe.subscribeToTopic(eq("NetworkQueryApp"), isA(KoalaPastryScribeApplicationBase.class))).thenReturn(topic);

        koalaPastryScribeApplication = new KoalaPastryScribeApplicationBase() {
            private ApplicationActivator applicationActivator = mock(AlwaysOnApplicationActivator.class);

            @Override
            public boolean becomeActive() {
                return true;
            }

            @Override
            public void deliver(PId id, ReceivedMessageContext messageContext) {
                // TODO Auto-generated method stub
            }

            @Override
            public void deliver(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity data) {
                // TODO Auto-generated method stub
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }

            @Override
            public void becomePassive() {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean handleAnycast(PubSubMessageContext pubSubMessageContext, EntityMethod entityMethod, PiEntity piEntity) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public ApplicationActivator getApplicationActivator() {
                return applicationActivator;
            }

            @Override
            public int getActivationCheckPeriodSecs() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public long getStartTimeout() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public TimeUnit getStartTimeoutUnit() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getApplicationName() {
                return "pi-app-name";
            }

            @Override
            protected PastryNode getPastryNode() {
                return node;
            }
        };
        koalaPastryScribeApplication.setScribe(scribe);
        koalaPastryScribeApplication.setKoalaJsonParser(new KoalaJsonParser());
        koalaPastryScribeApplication.setKoalaIdFactory(idFactory);

        koalaScribeDataParser = mock(KoalaPiEntityFactory.class);
        when(koalaScribeDataParser.getJson(isA(PiEntity.class))).thenReturn("yay");

        koalaPastryScribeApplication.setKoalaPiEntityFactory(koalaScribeDataParser);
    }

    @After
    public void after() {
        LogHelper.resetLogging();
    }

    @Test
    public void testGetKoalaScribeDataParser() {
        // act & assert
        assertEquals(koalaScribeDataParser, koalaPastryScribeApplication.getKoalaPiEntityFactory());
    }

    @Test
    public void testFailedTopic() {
        // setup
        ArrayList<Topic> topics = new ArrayList<Topic>();
        koalaPastryScribeApplication.subscribe(idFactory.convertToPId(topic.getId()), koalaPastryScribeApplication);
        topics.add(topic);

        // act
        koalaPastryScribeApplication.subscribeFailed(topics);

        // assert
        assertEquals(0, koalaPastryScribeApplication.getSubscribedTopics().size());
    }

    @Test
    public void testStart() {

        // act
        koalaPastryScribeApplication.start(node, null, null, null);

        // verify
        verify(node).buildEndpoint(isA(KoalaPastryScribeApplicationBase.class), isA(String.class));
    }

    @Test
    public void testScribeDeliver() {
        // setup
        KoalaPiEntityFactory testKoalaScribeDataParser = new KoalaPiEntityFactory();
        testKoalaScribeDataParser.setKoalaJsonParser(new KoalaJsonParser());
        ArrayList<PiEntity> koalaScribeDataTypes = new ArrayList<PiEntity>();
        koalaScribeDataTypes.add(new TestPiEntity());
        testKoalaScribeDataParser.setPiEntityTypes(koalaScribeDataTypes);
        koalaPastryScribeApplication.setKoalaPiEntityFactory(testKoalaScribeDataParser);
        String json = testKoalaScribeDataParser.getJson(new TestPiEntity("deliver"));

        // act
        koalaPastryScribeApplication.deliver(topic, new KoalaScribeContent(nodeHandle, transactionUID, EntityMethod.CREATE, json));

        // assert
        System.err.println(topic.toString());

        for (int i = 0; i < VectorAppender.getMessages().size(); i++) {
            System.err.println(i + " - " + VectorAppender.getMessages().elementAt(i));
        }

        List<String> logMessage = VectorAppender.getMessages();
        assertTrue(listContains(logMessage, "\"type\" : \"TestPiEntity\""));
        assertTrue(listContains(logMessage, topic.toString()));
        assertTrue(listContains(logMessage, nodeHandle.toString()));
    }

    private boolean listContains(List<String> stringList, String stringToCheck) {
        boolean result = false;
        for (String str : stringList) {
            if (str.contains(stringToCheck)) {
                result = true;
                break;
            }
        }
        return result;
    }

    @Test
    public void testSubscribefailed() {
        // setup
        Topic subscribedTopic = new Topic(koalaPastryScribeApplication.getKoalaIdFactory().buildId(testEntityTopicPId));
        koalaPastryScribeApplication.getSubscribedTopics().add(subscribedTopic);

        // act
        koalaPastryScribeApplication.subscribeFailed(subscribedTopic);

        // assert
        assertEquals(0, koalaPastryScribeApplication.getSubscribedTopics().size());
    }

    @Test
    public void testSubscribeToTopic() {
        // setup
        Topic subscribedTopic = new Topic(koalaPastryScribeApplication.getKoalaIdFactory().buildId(testEntityTopicPId));

        // act
        koalaPastryScribeApplication.subscribe(testEntityTopicPId, koalaPastryScribeApplication);

        // assert
        verify(scribe).subscribe(eq(subscribedTopic), eq(koalaPastryScribeApplication));
        assertEquals(1, koalaPastryScribeApplication.getSubscribedTopics().size());
    }

    @Test
    public void testUnsubscribeFromTopic() {
        // setup
        koalaPastryScribeApplication.getSubscribedTopics().add(topic);

        // act
        koalaPastryScribeApplication.unsubscribe(testEntityTopicPId, koalaPastryScribeApplication);

        // assert
        assertEquals(0, koalaPastryScribeApplication.getSubscribedTopics().size());
        verify(scribe).unsubscribe(topic, koalaPastryScribeApplication);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testAnycast() {
        // setup
        KoalaPiEntityFactory testKoalaScribeDataParser = new KoalaPiEntityFactory();
        testKoalaScribeDataParser.setKoalaJsonParser(new KoalaJsonParser());
        ArrayList<PiEntity> koalaScribeDataTypes = new ArrayList<PiEntity>();
        koalaScribeDataTypes.add(new TestPiEntity());
        testKoalaScribeDataParser.setPiEntityTypes(koalaScribeDataTypes);
        koalaPastryScribeApplication.setKoalaPiEntityFactory(testKoalaScribeDataParser);
        String json = testKoalaScribeDataParser.getJson(new TestPiEntity("anycast"));

        // act
        ScribeContent sc = new KoalaScribeContent(nodeHandle, transactionUID, EntityMethod.CREATE, json);
        koalaPastryScribeApplication.anycast(topic, sc);

        // assert
        List<String> logMessages = VectorAppender.getMessages();
        assertTrue(listContains(logMessages, "\"type\" : \"TestPiEntity\""));
        assertTrue(listContains(logMessages, topic.toString()));
        assertTrue(listContains(logMessages, nodeHandle.toString()));
    }

    @SuppressWarnings("deprecation")
    @Test(expected = RuntimeException.class)
    public void testAnycastWithBadContent() {
        // setup
        ScribeContent sc = mock(ScribeContent.class);

        // act
        koalaPastryScribeApplication.anycast(topic, sc);
    }

    @Test
    public void shouldGenerateNewPubSubMessageContext() {
        // act
        PubSubMessageContext res = koalaPastryScribeApplication.newPubSubMessageContext(testEntityTopicPId, "bbb");

        // assert
        assertSame(koalaPastryScribeApplication, res.getHandlingApplication());
        assertEquals(topic, res.getTopic());
        assertEquals(nodeHandle, res.getNodeHandle());
        assertEquals("bbb", res.getTransactionUID());
    }

    @Test
    public void shouldGenerateNewPubSubMessageContextWithExistingTransactionUid() {
        // act
        PubSubMessageContext res = koalaPastryScribeApplication.newPubSubMessageContext(testEntityTopicPId, nodeHandle, "bbb");

        // assert
        assertSame(koalaPastryScribeApplication, res.getHandlingApplication());
        assertEquals(topic, res.getTopic());
        assertEquals(nodeHandle, res.getNodeHandle());
        assertEquals("bbb", res.getTransactionUID());
    }

    @Test
    public void shouldGenerateNewPubSubMessageContextWithExistingTopicAndTransactionUid() {
        // act
        PubSubMessageContext res = koalaPastryScribeApplication.newPubSubMessageContext(testEntityTopicPId, nodeHandle, EXISTING_TX_UID);

        // assert
        assertSame(koalaPastryScribeApplication, res.getHandlingApplication());
        assertEquals(topic, res.getTopic());
        assertEquals(nodeHandle, res.getNodeHandle());
        assertEquals(EXISTING_TX_UID, res.getTransactionUID());
    }

    @Test
    public void destroyShouldUnsubScribeFromTopics() {
        // setup
        koalaPastryScribeApplication.getSubscribedTopics().add(topic);

        // act
        koalaPastryScribeApplication.destroy();

        // verify
        verify(scribe).unsubscribe(eq(koalaPastryScribeApplication.getSubscribedTopics()), eq(koalaPastryScribeApplication));
    }

    public static class TestPiEntity extends PiEntityBase {
        private String appMessage;

        public TestPiEntity() {
        }

        public TestPiEntity(String aAppMessage) {
            appMessage = aAppMessage;
        }

        public String getAppMessage() {
            return appMessage;
        }

        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return getClass().getSimpleName();
        }
    }
}

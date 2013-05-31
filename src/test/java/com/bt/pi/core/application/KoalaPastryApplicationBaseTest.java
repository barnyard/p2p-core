package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.environment.Environment;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.routing.Router;

import com.bt.pi.core.application.activation.ActivationAwareApplication;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.activation.UnknownApplicationException;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.message.KoalaMessageBase;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.messaging.KoalaMessageDeserializer;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.routing.RoutingMessageRedirector;

public class KoalaPastryApplicationBaseTest {
    private static final rice.pastry.Id NODE_ID = rice.pastry.Id.build("testNode");
    private KoalaPastryApplicationBase koalaPastryApplication;
    private PastryNode pastryNode;
    private Endpoint endpoint;
    private PId someId;
    private KoalaMessage koalaMessage;
    private ContinuationRequestWrapperImpl mockContinuationRequestWrapper;
    private TimeSource timeSource;
    private long currentTime;
    private Environment environment;
    private NodeHandle localNodeHandle;
    private PiEntity testPayload;
    private PiEntity deliveredPayload;
    private PiEntity payloadToForward;
    private KoalaJsonParser parser;
    private KoalaDHTStorage storage;
    private EchoApplication echoApplication;
    private Id redirectedForwardNodeId;
    private MessageForwardAction messageForwardAction;
    private boolean isForThisNode;
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private CountDownLatch deliveredLatch;
    private RoutingMessageRedirector routingMessageRedirector;
    private ApplicationActivator applicationActivator;
    private CountDownLatch applicationActivatorLatch;
    private ReceivedMessageContext deliveredMessageContext;
    private CountDownLatch applicationStartingLatch;
    private CountDownLatch applicationShuttingDownLatch;
    private String departedNodeId;
    private String arrivedNodeId;
    private NodeHandle departingNodeHandle;
    private NodeHandle arrivingNodeHandle;
    private NodeHandle neighboringNodeHandle;
    private CountDownLatch becomePassiveLatch;
    private KoalaIdFactory koalaIdFactory;

    @Before
    public void before() {

        ArrayList<PiEntity> payloads = new ArrayList<PiEntity>();
        payloads.add(new EchoPayload());

        ArrayList<KoalaMessageBase> messages = new ArrayList<KoalaMessageBase>();
        messages.add(new ApplicationMessage());

        parser = new KoalaJsonParser();

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(parser);
        koalaPiEntityFactory.setPiEntityTypes(payloads);

        koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);

        redirectedForwardNodeId = koalaIdFactory.buildId("redirect");

        deliveredLatch = new CountDownLatch(1);
        currentTime = 1208L;

        departedNodeId = null;

        testPayload = new EchoPayload();

        timeSource = mock(TimeSource.class);
        when(timeSource.currentTimeMillis()).thenReturn(currentTime);

        environment = mock(Environment.class);
        when(environment.getTimeSource()).thenReturn(timeSource);

        localNodeHandle = mock(NodeHandle.class);
        when(localNodeHandle.getNodeId()).thenReturn(NODE_ID);

        Id departingNodePastryId = rice.pastry.Id.build("departer");
        departingNodeHandle = mock(NodeHandle.class);
        when(departingNodeHandle.getNodeId()).thenReturn((rice.pastry.Id) departingNodePastryId);
        when(departingNodeHandle.getId()).thenReturn(departingNodePastryId);

        Id arrivingNodePastryId = rice.pastry.Id.build("arriver");
        arrivingNodeHandle = mock(NodeHandle.class);
        when(arrivingNodeHandle.getNodeId()).thenReturn((rice.pastry.Id) arrivingNodePastryId);
        when(arrivingNodeHandle.getId()).thenReturn(arrivingNodePastryId);

        Id neighboringNodeHandleId = rice.pastry.Id.build("neighbor");
        neighboringNodeHandle = mock(NodeHandle.class);
        when(neighboringNodeHandle.getNodeId()).thenReturn((rice.pastry.Id) neighboringNodeHandleId);
        when(neighboringNodeHandle.getId()).thenReturn(neighboringNodeHandleId);

        applicationStartingLatch = new CountDownLatch(1);
        applicationShuttingDownLatch = new CountDownLatch(1);
        becomePassiveLatch = new CountDownLatch(1);
        applicationActivatorLatch = new CountDownLatch(1);
        applicationActivator = new ApplicationActivator() {
            @Override
            public void register(ActivationAwareApplication app) {
                applicationActivatorLatch.countDown();
            }

            @Override
            public void deActivateNode(String id, ActivationAwareApplication anActivationAwareApplication) {
                // TODO Auto-generated method stub

            }

            @Override
            public ApplicationStatus getApplicationStatus(String appName) {
                return null;
            }
        };

        storage = mock(KoalaDHTStorage.class);

        ArrayList<NodeHandle> bestRoutingNodes = new ArrayList<NodeHandle>();
        bestRoutingNodes.add(neighboringNodeHandle);
        bestRoutingNodes.add(localNodeHandle);

        Router router = mock(Router.class);
        when(router.getBestRoutingCandidates((rice.pastry.Id) eq(departingNodePastryId))).thenReturn(bestRoutingNodes.iterator());

        someId = new PiId(rice.pastry.Id.build("AnId").toStringFull(), 0);
        koalaMessage = mock(KoalaMessage.class);
        endpoint = mock(Endpoint.class);
        mockContinuationRequestWrapper = mock(ContinuationRequestWrapperImpl.class);
        pastryNode = mock(PastryNode.class);
        when(pastryNode.buildEndpoint(isA(Application.class), isA(String.class))).thenReturn(endpoint);
        when(pastryNode.getEnvironment()).thenReturn(environment);
        when(pastryNode.getLocalNodeHandle()).thenReturn(localNodeHandle);
        when(pastryNode.getId()).thenReturn(NODE_ID);
        when(pastryNode.getNodeId()).thenReturn(NODE_ID);
        when(pastryNode.getRouter()).thenReturn(router);

        routingMessageRedirector = mock(RoutingMessageRedirector.class);

        messageForwardAction = new MessageForwardAction(false);

        threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.initialize();

        echoApplication = spy(new EchoApplication());
        when(echoApplication.getApplicationActivator()).thenReturn(applicationActivator);

        koalaPastryApplication = new KoalaPastryApplicationBase() {
            @Override
            public boolean becomeActive() {
                return true;
            }

            @Override
            public void deliver(PId id, ReceivedMessageContext messageContext) {
                deliveredMessageContext = messageContext;
                deliveredLatch.countDown();
            }

            @Override
            public MessageForwardAction forwardPiMessage(boolean isForThisNode, Id id, PiEntity payload) {
                KoalaPastryApplicationBaseTest.this.isForThisNode = isForThisNode;
                payloadToForward = payload;
                return messageForwardAction;
            }

            @Override
            protected PastryNode getPastryNode() {
                return pastryNode;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
                departedNodeId = nodeId;
            }

            @Override
            public void handleNodeArrival(String nodeId) {
                arrivedNodeId = nodeId;
            }

            @Override
            public void becomePassive() {
                becomePassiveLatch.countDown();
            }

            @Override
            protected RoutingMessageRedirector getRoutingMessageRedirector() {
                return routingMessageRedirector;
            }

            @Override
            public ApplicationActivator getApplicationActivator() {
                return applicationActivator;
            }

            @Override
            public int getActivationCheckPeriodSecs() {
                return 0;
            }

            @Override
            public long getStartTimeout() {
                return 0;
            }

            @Override
            public TimeUnit getStartTimeoutUnit() {
                return null;
            }

            @Override
            public String getApplicationName() {
                return "pi-app-name";
            }

            @Override
            protected void onApplicationStarting() {
                applicationStartingLatch.countDown();
            }

            @Override
            protected void onApplicationShuttingDown() {
                applicationShuttingDownLatch.countDown();
            }
        };

        HashMap<String, KoalaPastryApplicationBase> applicationMap = new HashMap<String, KoalaPastryApplicationBase>();
        applicationMap.put(koalaPastryApplication.getApplicationName(), koalaPastryApplication);
        applicationMap.put(echoApplication.getApplicationName(), echoApplication);

        setupKoalaPastryApplication(koalaPastryApplication, koalaPiEntityFactory, applicationMap);
        setupKoalaPastryApplication(echoApplication, koalaPiEntityFactory, applicationMap);
    }

    private void setupKoalaPastryApplication(KoalaPastryApplicationBase koalaPastryApplicationBase, KoalaPiEntityFactory koalaPiEntityFactory, HashMap<String, KoalaPastryApplicationBase> applicationMap) {
        koalaPastryApplicationBase.setDeserializer(mock(KoalaMessageDeserializer.class));
        koalaPastryApplicationBase.setKoalaIdFactory(koalaIdFactory);
        koalaPastryApplicationBase.setKoalaJsonParser(new KoalaJsonParser());
        koalaPastryApplicationBase.setKoalaPiEntityFactory(koalaPiEntityFactory);
        koalaPastryApplicationBase.setContinuationRequestWrapper(mockContinuationRequestWrapper);
        koalaPastryApplicationBase.setThreadPoolTaskExecutor(threadPoolTaskExecutor);

        koalaPastryApplicationBase.start(pastryNode, storage, applicationMap, null);
    }

    @Test
    public void shouldReturnTheCorrectPastryNode() {
        assertEquals(pastryNode, koalaPastryApplication.getPastryNode());
    }

    /**
     * Should have default deserializer
     */
    @Test
    public void shouldHaveDefaultDeserializer() throws Exception {
        // assert
        verify(endpoint, atLeastOnce()).setDeserializer(isA(KoalaMessageDeserializer.class));
    }

    /**
     * Should be able to inject own deserializer
     */
    @Test
    public void shouldBeAbleToInjectOwnDeserializer() throws Exception {
        // setup
        MessageDeserializer duffDeserializer = mock(MessageDeserializer.class);

        // act
        koalaPastryApplication.setDeserializer(duffDeserializer);

        // assert
        verify(endpoint).setDeserializer(duffDeserializer);
    }

    @Test
    public void shouldGetTimeSourceFromPastryNodeEnvironment() {
        // act & assert
        assertEquals(timeSource, koalaPastryApplication.getTimeSource());
    }

    @Test
    public void shouldGetTimeStampFromPastryNodeEnvironment() {
        // act & assert
        assertEquals(currentTime, koalaPastryApplication.getTimeStamp());
    }

    @Test
    public void shouldDeliverTheMessageToContinuationWrappers() {
        // setup
        koalaPastryApplication.setContinuationRequestWrapper(mockContinuationRequestWrapper);

        // act
        koalaPastryApplication.deliverToWrappers(someId, koalaMessage);

        // verify
        verify(mockContinuationRequestWrapper).messageReceived(eq(someId), eq(koalaMessage));
    }

    @Test
    public void shouldReturnSetKoalaJsonParser() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        koalaPastryApplication.setKoalaJsonParser(koalaJsonParser);

        // assert
        assertEquals(koalaJsonParser, koalaPastryApplication.getKoalaJsonParser());
    }

    @Test
    public void shouldReturnNodeHandleFromPastryNode() {

        // act & assert
        assertEquals(localNodeHandle, koalaPastryApplication.getNodeHandle());
    }

    @Test
    public void getNodeIdFullShouldReturnFullStringNodeIdFromPastryNode() {
        // act & assert
        assertEquals(NODE_ID.toStringFull(), koalaPastryApplication.getNodeIdFull());
    }

    @Test
    public void shouldCallApplicationPayloadDeliverforDeliveringApplicationMessages() throws Exception {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), null, null, null, null);

        // act
        koalaPastryApplication.deliver(rice.pastry.Id.build(someId.toStringFull()), appMessage);

        // assert
        assertTrue(deliveredLatch.await(5, TimeUnit.SECONDS));
        assertEquals(koalaPastryApplication, deliveredMessageContext.getHandlingApplication());
        assertEquals(appMessage, deliveredMessageContext.getApplicationMessage());
    }

    @Test
    public void shouldDeliverMessageToEchoApp() {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), EntityMethod.CREATE, EntityResponseCode.OK, echoApplication.getApplicationName(), koalaPastryApplication.getApplicationName());
        PId id = new PiId(rice.pastry.Id.build("appTastic").toStringFull(), 0);

        // act
        koalaPastryApplication.deliver(id, appMessage);

        // verify
        verify(echoApplication).deliver(eq(id), isA(ReceivedMessageContext.class));
        assertNull(deliveredPayload);
    }

    @Test
    public void shouldDeliverMessageIfTheDestinationAppIsSameAsSourceApp() throws Exception {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), EntityMethod.CREATE, EntityResponseCode.OK, koalaPastryApplication.getApplicationName(), koalaPastryApplication
                .getApplicationName());

        // act
        koalaPastryApplication.deliver(rice.pastry.Id.build("appTastic"), appMessage);

        // assert
        assertTrue(deliveredLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(deliveredMessageContext);
        verify(echoApplication, never()).deliver(isA(PId.class), isA(ReceivedMessageContext.class));
    }

    @Test
    public void shouldForwardUndeserializableMessage() throws Exception {
        // setup
        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenThrow(new IOException("oops"));

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // verify
        assertTrue(res);
    }

    @Test
    public void shouldForwardUnknownMessage() throws Exception {
        // setup
        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(null);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // verify
        assertTrue(res);
    }

    @Test
    public void shouldForwardMessageToEchoApp() throws Exception {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(testPayload), someId.toStringFull(), EntityMethod.CREATE, null, echoApplication.getApplicationName(), koalaPastryApplication.getApplicationName());

        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);
        when(routeMessage.getDestinationId()).thenReturn(NODE_ID);
        when(routeMessage.getNextHop()).thenReturn(localNodeHandle);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // verify
        assertTrue(res);
        assertNull(payloadToForward);
    }

    @Test
    public void shouldForwardIfDestinationAppSameAsSourceApp() throws Exception {
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(testPayload), someId.toStringFull(), EntityMethod.CREATE, null, koalaPastryApplication.getApplicationName(), koalaPastryApplication.getApplicationName());

        rice.pastry.NodeHandle someOtherNodeHandle = mock(rice.pastry.NodeHandle.class);
        when(someOtherNodeHandle.getNodeId()).thenReturn(rice.pastry.Id.build("unknown"));

        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);
        when(routeMessage.getNextHop()).thenReturn(someOtherNodeHandle);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // assert
        assertNotNull(payloadToForward);
        assertFalse(isForThisNode);
        assertFalse(res);
    }

    @Test
    public void shouldForwardIfDestinationAppSameAsSourceAppAndThisIsTheDestinationNode() throws Exception {
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(testPayload), someId.toStringFull(), EntityMethod.CREATE, null, koalaPastryApplication.getApplicationName(), koalaPastryApplication.getApplicationName());

        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);
        when(routeMessage.getDestinationId()).thenReturn(NODE_ID);
        when(routeMessage.getNextHop()).thenReturn(localNodeHandle);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // assert
        assertNotNull(payloadToForward);
        assertTrue(isForThisNode);
        assertFalse(res);
    }

    @Test
    public void shouldChangeDestinationNodeIdOnForwardIfSpecified() throws Exception {
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(testPayload), someId.toStringFull(), EntityMethod.CREATE, null, koalaPastryApplication.getApplicationName(), koalaPastryApplication.getApplicationName());

        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);
        when(routeMessage.getNextHop()).thenReturn(localNodeHandle);

        messageForwardAction = new MessageForwardAction(true, redirectedForwardNodeId);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // assert
        verify(routingMessageRedirector).reroute(routeMessage, redirectedForwardNodeId);
        assertTrue(res);
    }

    @Test
    public void shouldForwardIfUnknownDestinationApp() throws Exception {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), EntityMethod.CREATE, null, "MOOOOO", koalaPastryApplication.getApplicationName());

        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);

        // act
        boolean res = koalaPastryApplication.forward(routeMessage);

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldThrowIfDeliveringToUnknownDestinationApp() {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), EntityMethod.CREATE, null, "MOOOOO", koalaPastryApplication.getApplicationName());

        // act
        try {
            koalaPastryApplication.deliver(someId, appMessage);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof UnknownApplicationException);
        }
    }

    @Test
    public void shouldDeliverMethodToContinuousWrapperAndNotToChildDeliver() {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), EntityMethod.CREATE, null, echoApplication.getApplicationName(), koalaPastryApplication.getApplicationName());
        when(mockContinuationRequestWrapper.messageReceived(someId, appMessage)).thenReturn(true);

        // act
        koalaPastryApplication.deliver(someId, appMessage);

        //
        assertNull(deliveredPayload);
    }

    @Test
    public void shouldDeliverMethodToBlockingWrapperAndNotToChildDeliver() {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), null, null, echoApplication.getApplicationName(), koalaPastryApplication.getApplicationName());

        // act
        koalaPastryApplication.deliver(someId, appMessage);

        //
        assertNull(deliveredPayload);
    }

    @Test
    public void shouldCallApplicationPayloadForwardforDeliveringApplicationMessages() throws Exception {
        // setup
        ApplicationMessage appMessage = new ApplicationMessage(parser.getJson(new EchoPayload()), someId.toStringFull(), null, null, null, null);
        rice.pastry.routing.RouteMessage routeMessage = mock(rice.pastry.routing.RouteMessage.class);
        when(routeMessage.getMessage(isA(MessageDeserializer.class))).thenReturn(appMessage);
        when(routeMessage.getNextHop()).thenReturn(localNodeHandle);

        // act
        koalaPastryApplication.forward(routeMessage);

        // assert
        assertTrue(payloadToForward instanceof EchoPayload);
    }

    @Test
    public void shouldCallBespokeStartHandlerWhenStartInvoked() throws Exception {
        // act
        koalaPastryApplication.start(pastryNode, null, null, null);

        // assert
        assertTrue(applicationStartingLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldRegisterWithActivatorWhenStartInvoked() throws Exception {
        // act
        koalaPastryApplication.start(pastryNode, null, null, null);

        // assert
        assertTrue(applicationActivatorLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldCallAndBespokeShutdownHandlerWhenAppContextShuttingDown() throws Exception {
        // act
        koalaPastryApplication.applicationContextShuttingDown();

        // assert
        assertTrue(applicationShuttingDownLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldSetStorageClass() {
        // setup
        KoalaDHTStorage dummyStorage = mock(KoalaDHTStorage.class);

        // act
        koalaPastryApplication.setStorage(dummyStorage);

        // assert
        assertEquals(dummyStorage, koalaPastryApplication.getStorage());
    }

    @Test
    public void shouldCreateNewMessageContext() {
        // act
        MessageContext res = koalaPastryApplication.newMessageContext();

        // assert
        assertEquals(koalaPastryApplication, res.getHandlingApplication());
    }

    @Test
    public void shouldCreateNewMessageContextWithTransactionUID() {
        // act
        MessageContext res = koalaPastryApplication.newMessageContext("aaa");

        // assert
        assertEquals(koalaPastryApplication, res.getHandlingApplication());
        assertEquals("aaa", res.getTransactionUID());
    }

    @Test
    public void shouldNotDeliverNodeIdToHandleDepartureBecauseNodeHasJoined() {
        // act
        koalaPastryApplication.update(departingNodeHandle, true);

        // assert
        assertNull(departedNodeId);
    }

    @Test
    public void shouldNotNotifyAppOfDepartingNodeWhenNodeIsNotNearest() {
        when(neighboringNodeHandle.checkLiveness()).thenReturn(true);

        // act
        koalaPastryApplication.update(departingNodeHandle, false);

        // assert
        assertNull(departedNodeId);
    }

    @Test
    public void shouldNotifyAppOfDepartingNodeWhenItIsNearest() {
        when(neighboringNodeHandle.checkLiveness()).thenReturn(false);

        // act
        koalaPastryApplication.update(departingNodeHandle, false);

        // assert
        assertEquals(departingNodeHandle.getId().toStringFull(), departedNodeId);
    }

    @Test
    public void shouldNotifyAppOfNewNodeArriving() {
        // act
        koalaPastryApplication.update(arrivingNodeHandle, true);

        // assert
        assertEquals(arrivingNodeHandle.getId().toStringFull(), arrivedNodeId);
    }

    @Test
    public void shouldReturnApplicationStatus() {
        koalaPastryApplication.getApplicationStatus();
    }

    @Test
    public void shouldGetContinuationRequestWrapper() {
        koalaPastryApplication.getContinuationRequestWrapper();
    }
}

package com.bt.pi.core.application.reporter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.PastryNode;

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class ReportingApplicationTest {
    private static final String NODE_ID = "nodeId";

    @Mock
    private PId id;
    @Mock
    private PId superNodeId;
    @Mock
    private Id nodeId;
    @Mock
    private PId superNodeApplicationCheckPointsId;
    @Mock
    private SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;
    @Mock
    private ReportHandler<ReportableEntity<?>> logMessageHandler;
    @Mock
    private ReportHandler<ReportableEntity<?>> nodePhysicalHealthHandler;
    @Mock
    private PiEntityCollection<LogMessageEntity> logMessageEntities;
    @Mock
    private KoalaIdFactory idFactory;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private MessageContext messageContext;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private CountDownLatch latch;

    @InjectMocks
    private ReportingApplication reportingApplication = spy(new ReportingApplication() {
        protected rice.pastry.PastryNode getPastryNode() {
            return mock(PastryNode.class);
        };
    });

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        when(idFactory.buildPId("topic:report")).thenReturn(id);
        when(id.forLocalScope(NodeScope.AVAILABILITY_ZONE)).thenReturn(id);
        when(idFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(superNodeApplicationCheckPointsId);
        when(idFactory.buildPIdFromHexString("superNodeId")).thenReturn(superNodeId);
        when(idFactory.getRegion()).thenReturn(1);
        when(idFactory.getAvailabilityZoneWithinRegion()).thenReturn(2);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PiContinuation<SuperNodeApplicationCheckPoints>) invocation.getArguments()[1]).handleResult(superNodeApplicationCheckPoints);
                return null;
            }
        }).when(dhtCache).get(eq(superNodeApplicationCheckPointsId), isA(PiContinuation.class));

        when(id.toStringFull()).thenReturn(NODE_ID);

        when(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, 1, 2)).thenReturn("superNodeId");

        NodeHandle nodeHandle = mock(NodeHandle.class);
        when(nodeHandle.getId()).thenReturn(nodeId);
        when(pubSubMessageContext.getNodeHandle()).thenReturn(nodeHandle);

        setupThreadPool();

        when(logMessageHandler.getReportableEntityTypesHandled()).thenReturn(Arrays.asList(new String[] { "LogMessageEntityCollection" }));
        when(nodePhysicalHealthHandler.getReportableEntityTypesHandled()).thenReturn(Arrays.asList(new String[] { new HeartbeatEntity(null).getType(), new HeartbeatEntityCollection().getType() }));

        List<ReportHandler<ReportableEntity<?>>> reportHandlers = new ArrayList<ReportHandler<ReportableEntity<?>>>();
        reportHandlers.add(logMessageHandler);
        reportHandlers.add(nodePhysicalHealthHandler);
        reportingApplication.setReportHandlers(reportHandlers);
    }

    private void setupThreadPool() {
        latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                latch.countDown();
                return null;
            }
        }).when(threadPoolTaskExecutor).execute(isA(Runnable.class));
    }

    @Test
    public void superNodeTopicScopeShouldBeAvailabilityZone() {
        // assert
        assertEquals(NodeScope.AVAILABILITY_ZONE, reportingApplication.getSuperNodeTopicScope());
    }

    @Test
    public void superNodeTopicUrlShouldBeHealth() {
        // assert
        assertEquals("topic:report", reportingApplication.getSuperNodeTopicUrl());
    }

    @Test
    public void applicationNameShouldBeHealthApp() {
        // assert
        assertEquals("pi-reporting-app", reportingApplication.getApplicationName());
    }

    @Test
    public void publishShouldBroadcastTheEntity() {
        // setup
        doAnswer(new Answer<PubSubMessageContext>() {
            @Override
            public PubSubMessageContext answer(InvocationOnMock invocation) throws Throwable {
                return pubSubMessageContext;
            }
        }).when(reportingApplication).newPubSubMessageContext(eq(id), isA(String.class));

        // act
        reportingApplication.publishToReportingTopic(logMessageEntities);

        // assert
        verify(pubSubMessageContext).publishContent(EntityMethod.UPDATE, logMessageEntities);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void broadcastLogMessageEntityCollectionShouldDelegateToLogMessageHandler() {
        // setup
        LogMessageEntityCollection data = mock(LogMessageEntityCollection.class);
        LogMessageEntity lme = mock(LogMessageEntity.class);
        when(data.getEntities()).thenReturn(Arrays.asList(lme));
        when(data.getType()).thenReturn("LogMessageEntityCollection");

        // act
        reportingApplication.deliver(pubSubMessageContext, EntityMethod.UPDATE, data);

        // assert
        verify(logMessageHandler).receive((PiEntityCollection) data, true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void broadcastHeartbeatEntityCollectionShouldDelegateToLogMessageHandler() {
        // setup
        HeartbeatEntityCollection data = mock(HeartbeatEntityCollection.class);
        HeartbeatEntity heartbeatEntity = mock(HeartbeatEntity.class);
        when(data.getEntities()).thenReturn(Arrays.asList(heartbeatEntity));
        when(data.getType()).thenReturn("HeartbeatEntityCollection");

        // act
        reportingApplication.deliver(pubSubMessageContext, EntityMethod.CREATE, data);

        // assert
        verify(nodePhysicalHealthHandler).receive((PiEntityCollection) data, true);
    }

    @Test
    public void publishUpdateOfLogMessageEntityCollection() {
        // setup
        LogMessageEntityCollection data = mock(LogMessageEntityCollection.class);

        doAnswer(new Answer<MessageContext>() {
            @Override
            public MessageContext answer(InvocationOnMock invocation) throws Throwable {
                return messageContext;
            }
        }).when(reportingApplication).newMessageContext(isA(String.class));

        // act
        reportingApplication.sendReportingUpdateToASuperNode(data);

        // assert
        verify(messageContext).routePiMessage(superNodeId, EntityMethod.UPDATE, data);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void publishUpdateOfLogMessageEntityCollectionWhenSuperNodesNotSeededDoesNothing() {
        // setup
        LogMessageEntityCollection data = mock(LogMessageEntityCollection.class);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PiContinuation<SuperNodeApplicationCheckPoints>) invocation.getArguments()[1]).handleResult(null);
                return null;
            }
        }).when(dhtCache).get(eq(superNodeApplicationCheckPointsId), isA(PiContinuation.class));

        // act
        reportingApplication.sendReportingUpdateToASuperNode(data);

        // assert
        verify(messageContext, never()).routePiMessage(superNodeId, EntityMethod.UPDATE, data);
    }

    @Test
    public void publishUpdateOfLogMessageEntityCollectionWhenHealthAppNotSeededDoesNothing() {
        // setup
        LogMessageEntityCollection data = mock(LogMessageEntityCollection.class);
        when(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, 1, 2)).thenReturn(null);

        // act
        reportingApplication.sendReportingUpdateToASuperNode(data);

        // assert
        verify(messageContext, never()).routePiMessage(superNodeId, EntityMethod.UPDATE, data);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleRequestForLogMessages() throws Exception {
        // setup
        final NavigableSet<LogMessageEntity> logEntities = new TreeSet<LogMessageEntity>();
        LogMessageEntityCollection logMessageEntityCollection = new LogMessageEntityCollection();
        logMessageEntityCollection.setEntities(logEntities);
        when(logMessageHandler.getAllEntities()).thenReturn((PiEntityCollection) logMessageEntityCollection);

        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.GET);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(new LogMessageEntityCollection());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(PiEntity.class));

        // act
        reportingApplication.deliver(id, receivedMessageContext);

        latch.await(400, TimeUnit.MILLISECONDS);

        // assert
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<PiEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                return ((LogMessageEntityCollection) argument).getEntities().equals(logEntities.descendingSet());
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleRequestForHeartbeats() throws Exception {
        // setup
        final Collection<HeartbeatEntity> entities = new ArrayList<HeartbeatEntity>();
        HeartbeatEntityCollection heartbeatEntityCollection = new HeartbeatEntityCollection();
        heartbeatEntityCollection.setEntities(entities);
        when(nodePhysicalHealthHandler.getAllEntities()).thenReturn((PiEntityCollection) heartbeatEntityCollection);

        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.GET);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(new HeartbeatEntityCollection());

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(PiEntity.class));

        // act
        reportingApplication.deliver(id, receivedMessageContext);

        latch.await(400, TimeUnit.MILLISECONDS);

        // assert
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<PiEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                return ((HeartbeatEntityCollection) argument).getEntities().equals(entities);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleRequestForNotEmptyPiEntityCollection() throws Exception {
        // setup
        final Collection<HeartbeatEntity> entities = new ArrayList<HeartbeatEntity>();
        entities.add(mock(HeartbeatEntity.class));
        HeartbeatEntityCollection heartbeatEntityCollection = new HeartbeatEntityCollection();
        heartbeatEntityCollection.setEntities(entities);
        when(nodePhysicalHealthHandler.getEntities(heartbeatEntityCollection)).thenReturn((PiEntityCollection) heartbeatEntityCollection);

        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.GET);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(heartbeatEntityCollection);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), isA(PiEntity.class));

        // act
        reportingApplication.deliver(id, receivedMessageContext);

        latch.await(400, TimeUnit.MILLISECONDS);

        // assert
        verify(nodePhysicalHealthHandler).getEntities(heartbeatEntityCollection);
        verify(receivedMessageContext).sendResponse(eq(EntityResponseCode.OK), argThat(new ArgumentMatcher<PiEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                return ((HeartbeatEntityCollection) argument).getEntities().equals(entities);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateOfLogMessagesShouldBeDelegated() throws Exception {
        // setup
        LogMessageEntityCollection logMessageEntityCollection = new LogMessageEntityCollection();
        logMessageEntityCollection.setEntities(Collections.EMPTY_LIST);
        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(logMessageEntityCollection);

        // act
        reportingApplication.deliver(id, receivedMessageContext);

        // assert
        verify(logMessageHandler).receive((PiEntityCollection) logMessageEntityCollection, false);
    }

    @Test
    public void updateOfHeartbeatsShouldBeDelegated() throws Exception {
        // setup
        HeartbeatEntity heartbeatEntity = new HeartbeatEntity(null);
        ReceivedMessageContext receivedMessageContext = mock(ReceivedMessageContext.class);
        when(receivedMessageContext.getMethod()).thenReturn(EntityMethod.UPDATE);
        when(receivedMessageContext.getReceivedEntity()).thenReturn(heartbeatEntity);

        // act
        reportingApplication.deliver(id, receivedMessageContext);

        // assert
        verify(nodePhysicalHealthHandler).receive(heartbeatEntity);
    }

}

package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.Topic;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.KoalaScribeImpl;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessage;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessageType;

public class PubSubMessageContextTest {
    private static final String JSON = "json";
    private static final String TRANSACTION_ID = "transaction-id";
    private PubSubMessageContext pubSubMessageContext;
    private KoalaPastryScribeApplicationBase handlingApplication;
    private Topic existingTopic;
    private NodeHandle nodeHandle;
    private NodeHandle existingNodeHandle;
    private KoalaScribeImpl scribe;
    private PiEntity piEntity;
    private KoalaIdFactory idFactory;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private PId id;
    private Id tId;
    private String url;
    private String topicId;
    private PiLocation topicEntity;

    @Before
    public void before() {

        url = "url";
        topicId = "AAABBBCCCDDDEEEFFF";

        handlingApplication = mock(KoalaPastryScribeApplicationBase.class);
        existingTopic = mock(Topic.class);
        nodeHandle = mock(NodeHandle.class);
        existingNodeHandle = mock(NodeHandle.class);
        scribe = mock(KoalaScribeImpl.class);
        piEntity = mock(PiEntity.class);

        id = mock(PId.class);
        when(id.forScope(isA(NodeScope.class), anyInt())).thenReturn(id);
        when(id.forLocalScope(isA(NodeScope.class))).thenReturn(id);
        when(id.getIdAsHex()).thenReturn(topicId);
        when(id.forGlobalAvailablityZoneCode(PId.getGlobalAvailabilityZoneCodeFromId(topicId))).thenReturn(id);

        tId = mock(Id.class);
        when(tId.toStringFull()).thenReturn(topicId);

        topicEntity = mock(PiLocation.class);
        when(topicEntity.getUrl()).thenReturn("topic:url");
        when(topicEntity.getNodeScope()).thenReturn(NodeScope.REGION);

        koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);
        when(koalaPiEntityFactory.getJson(piEntity)).thenReturn(JSON);

        when(existingTopic.getId()).thenReturn(tId);
        when(piEntity.getUrl()).thenReturn(url);

        idFactory = mock(KoalaIdFactory.class);
        when(idFactory.buildPId(anyString())).thenReturn(id);
        when(idFactory.buildId(id)).thenReturn(tId);

        when(handlingApplication.getNodeHandle()).thenReturn(nodeHandle);
        when(handlingApplication.getScribe()).thenReturn(scribe);
        when(handlingApplication.getKoalaPiEntityFactory()).thenReturn(koalaPiEntityFactory);
        when(handlingApplication.getKoalaIdFactory()).thenReturn(idFactory);

        pubSubMessageContext = new PubSubMessageContext(handlingApplication, existingTopic, existingNodeHandle, TRANSACTION_ID);
    }

    @Test
    public void constructorsShouldBeEquivalent() {
        // act
        PubSubMessageContext scopedEntityContext = new PubSubMessageContext(handlingApplication, topicEntity, TRANSACTION_ID);
        PubSubMessageContext topicContext = new PubSubMessageContext(handlingApplication, new Topic(tId), handlingApplication.getNodeHandle(), TRANSACTION_ID);

        // assert
        assertEquals(scopedEntityContext, topicContext);
    }

    @Test
    public void shouldAllowConstructionWithTopicEntityAndTransactionUID() {
        // act
        pubSubMessageContext = new PubSubMessageContext(handlingApplication, topicEntity, "aa");

        // assert
        assertEquals(new Topic(tId), pubSubMessageContext.getTopic());
        assertEquals("aa", pubSubMessageContext.getTransactionUID());
    }

    @Test
    public void shouldAllowConstructionFromExistingTopicNodeHandleAndTransactionId() {
        // assert
        assertEquals(existingTopic, pubSubMessageContext.getTopic());
        assertEquals(existingNodeHandle, pubSubMessageContext.getNodeHandle());
        assertEquals(TRANSACTION_ID, pubSubMessageContext.getTransactionUID());
    }

    @Test
    public void shouldBeAbleToSendAnycast() {
        // act
        pubSubMessageContext.sendAnycast(EntityMethod.CREATE, piEntity);

        // verify
        verify(scribe).sendAnycast(eq(existingTopic), argThat(new ArgumentMatcher<KoalaScribeContent>() {
            public boolean matches(Object argument) {
                KoalaScribeContent arg = (KoalaScribeContent) argument;
                assertEquals(EntityMethod.CREATE, arg.getEntityMethod());
                assertEquals(JSON, arg.getJsonData());
                assertEquals(existingNodeHandle, arg.getSourceNodeHandle());
                assertEquals(TRANSACTION_ID, arg.getTransactionUID());
                return true;
            }
        }));
    }

    @Test
    public void shouldBeAbleToPublishContent() {
        // act
        pubSubMessageContext.publishContent(EntityMethod.CREATE, piEntity);

        // verify
        verify(scribe).publishContent(eq(existingTopic), argThat(new ArgumentMatcher<KoalaScribeContent>() {
            public boolean matches(Object argument) {
                KoalaScribeContent arg = (KoalaScribeContent) argument;
                assertEquals(EntityMethod.CREATE, arg.getEntityMethod());
                assertEquals(JSON, arg.getJsonData());
                assertEquals(existingNodeHandle, arg.getSourceNodeHandle());
                assertEquals(TRANSACTION_ID, arg.getTransactionUID());
                return true;
            }
        }));
    }

    @Test
    public void testRandomAnycast() throws Exception {
        // setup
        pubSubMessageContext = spy(pubSubMessageContext);
        when(idFactory.buildPId(isA(String.class))).thenReturn(id);
        doNothing().when(pubSubMessageContext).routeMessage(eq(id), isA(KoalaMessage.class));

        // act
        pubSubMessageContext.randomAnycast(EntityMethod.CREATE, piEntity);

        // assert
        verify(scribe).sendScribeMessageFromRandomOrigin(eq(id), argThat(new ArgumentMatcher<WrappedScribeContentMessage>() {
            @Override
            public boolean matches(Object arg0) {
                WrappedScribeContentMessage wrappedMessage = (WrappedScribeContentMessage) arg0;
                assertEquals(EntityMethod.CREATE, wrappedMessage.getEntityMethod());
                assertEquals(JSON, wrappedMessage.getJsonData());
                assertEquals(existingNodeHandle, wrappedMessage.getSource());
                assertEquals(WrappedScribeContentMessageType.ANYCAST, wrappedMessage.getWrappedScribeContentMessageType());
                return wrappedMessage.getTopic().equals(existingTopic);
            }
        }));
    }
}

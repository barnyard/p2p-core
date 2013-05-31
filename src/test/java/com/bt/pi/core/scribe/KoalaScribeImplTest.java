package com.bt.pi.core.scribe;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import rice.environment.Environment;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;
import rice.p2p.commonapi.rawserialization.RawMessage;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.messaging.AnycastMessage;
import rice.p2p.scribe.messaging.DropMessage;
import rice.p2p.scribe.messaging.SubscribeAckMessage;
import rice.p2p.scribe.messaging.SubscribeFailedMessage;
import rice.p2p.scribe.messaging.SubscribeMessage;
import rice.p2p.scribe.messaging.UnsubscribeMessage;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.direct.DirectNodeHandle;
import rice.pastry.direct.DirectPastryNodeFactory;
import rice.pastry.direct.EuclideanNetwork;
import rice.pastry.direct.NetworkSimulator;
import rice.pastry.standard.RandomNodeIdFactory;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scribe.content.KoalaScribeContent;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessage;
import com.bt.pi.core.scribe.content.WrappedScribeContentMessageType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(WrappedScribeContentMessage.class)
@PowerMockIgnore( { "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class KoalaScribeImplTest {
    private static final String DUMMY_NETWORK = "dummyNetwork";
    private static final String TOPIC_ID = "topicId";

    @Rule
    public TestName testName = new TestName();

    private KoalaScribeImpl scribe;
    private ScribeMultiClient scribeClient;
    private KoalaIdFactory koalaIdFactory;
    private Topic topic;
    private KoalaScribeContent scribeContent;
    private Id id;
    private Id topicId;
    private Endpoint endpoint;
    private WrappedScribeContentMessage wrappedScribeContentMessage;
    private NodeHandle nodeHandle;
    private InputBuffer inputBuffer;
    private MessageDeserializer messageDeserializer;
    private ApplicationContext applicationContext;

    @Before
    public void before() throws Exception {
        scribeClient = mock(ScribeMultiClient.class);
        topic = mock(Topic.class);
        id = mock(Id.class);
        topicId = mock(Id.class);
        endpoint = mock(Endpoint.class);
        scribeContent = mock(KoalaScribeContent.class);
        applicationContext = mock(ApplicationContext.class);
        wrappedScribeContentMessage = new WrappedScribeContentMessage(mock(NodeHandle.class), topic, WrappedScribeContentMessageType.ANYCAST, EntityMethod.UPDATE, "json", "transaction-id");

        koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.buildIdFromToString(TOPIC_ID)).thenReturn(topicId);

        scribe = spy(new KoalaScribeImpl(setupPastryNode(), applicationContext));
        scribe.setKoalaIdFactory(koalaIdFactory);

        messageDeserializer = scribe.getEndpoint().getDeserializer();
    }

    private PastryNode setupPastryNode() throws Exception {
        Environment environment = new Environment();
        NetworkSimulator<DirectNodeHandle, RawMessage> simulator = new EuclideanNetwork<DirectNodeHandle, RawMessage>(environment);
        PastryNodeFactory factory = new DirectPastryNodeFactory(new RandomNodeIdFactory(environment), simulator, environment);
        PastryNode pastryNode = spy(factory.newNode());

        if (testName.getMethodName().equals("testSendScribeMessageFromRandomOrigin"))
            doReturn(endpoint).when(pastryNode).buildEndpoint(isA(Application.class), isA(String.class));

        return pastryNode;
    }

    @Before
    public void setupForMessageDeserializerTests() throws Exception {
        nodeHandle = mock(NodeHandle.class);
        inputBuffer = mock(InputBuffer.class);
        when(inputBuffer.readByte()).thenReturn((byte) 1);
        when(inputBuffer.readShort()).thenReturn((short) 1);
        when(inputBuffer.readUTF()).thenReturn(WrappedScribeContentMessageType.ANYCAST.toString());
    }

    @Test
    public void testSubscribeToTopic() {
        // setup
        doReturn(topic).when(scribe).createTopic(DUMMY_NETWORK);
        doNothing().when(scribe).subscribe(topic, scribeClient);

        // act
        Topic result = scribe.subscribeToTopic(DUMMY_NETWORK, scribeClient);

        // assert
        assertThat(result, equalTo(topic));
        verify(scribe).subscribe(topic, scribeClient);
    }

    @Test
    public void testPublishContent() {
        // setup
        doNothing().when(scribe).publish(topic, scribeContent);

        // act
        scribe.publishContent(topic, scribeContent);

        // assert
        verify(scribe).publish(topic, scribeContent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPublishContentNullTopic() {
        // act
        scribe.publishContent(null, scribeContent);
    }

    @Test
    public void testSendAnycast() {
        // setup
        doNothing().when(scribe).anycast(topic, scribeContent);

        // act
        scribe.sendAnycast(topic, scribeContent);

        // assert
        verify(scribe).anycast(topic, scribeContent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSendAnycastNullTopic() {
        // act
        scribe.sendAnycast(null, scribeContent);
    }

    @Test
    public void testCreateTopic() {
        // setup
        PId topicPId = mock(PId.class);
        when(topicPId.forLocalScope(eq(NodeScope.REGION))).thenReturn(topicPId);
        when(koalaIdFactory.buildPId(DUMMY_NETWORK)).thenReturn(topicPId);
        when(koalaIdFactory.buildId(topicPId)).thenReturn(id);

        // act
        Topic result = scribe.createTopic(DUMMY_NETWORK);

        // assert
        assertThat(result.getId(), equalTo(id));
    }

    @Test
    public void testSendScribeMessageFromRandomOrigin() throws Exception {
        // setup
        PId topicPId = mock(PId.class);
        when(topicPId.forLocalScope(eq(NodeScope.REGION))).thenReturn(topicPId);
        when(koalaIdFactory.buildId(topicPId)).thenReturn(id);

        // act
        scribe.sendScribeMessageFromRandomOrigin(topicPId, wrappedScribeContentMessage);

        // assert
        verify(endpoint).route(id, wrappedScribeContentMessage, null);
    }

    @Test
    public void testDeliver() throws Exception {
        // setup
        doNothing().when(scribe).anycast(eq(topic), isA(ScribeContent.class));

        // act
        scribe.deliver(id, wrappedScribeContentMessage);

        // assert
        verify(scribe).anycast(eq(topic), argThat(new ArgumentMatcher<KoalaScribeContent>() {
            @Override
            public boolean matches(Object argument) {
                return ((KoalaScribeContent) argument).getJsonData().equals("json");
            }
        }));
    }

    @Test
    public void testMessageDeserializerWrappedMessage() throws Exception {// setup
        PowerMockito.mockStatic(WrappedScribeContentMessage.class);
        when(WrappedScribeContentMessage.build(isA(InputBuffer.class), isA(Endpoint.class))).thenReturn(wrappedScribeContentMessage);

        // act
        Message message = messageDeserializer.deserialize(inputBuffer, wrappedScribeContentMessage.getType(), 0, nodeHandle);

        // assert
        assertThat(message instanceof WrappedScribeContentMessage, is(true));
    }

    @Test
    public void testMessageDeserializerAnycastMessage() throws Exception {
        // act
        Message message = messageDeserializer.deserialize(inputBuffer, AnycastMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof AnycastMessage, is(true));
    }

    @Test
    public void testMessageDeserializerSubscribeMessage() throws Exception {
        // act
        Message message = messageDeserializer.deserialize(inputBuffer, SubscribeMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof SubscribeMessage, is(true));
    }

    @Test
    public void testMessageDeserializerSubscribeAckMessage() throws Exception {
        // act
        Message message = messageDeserializer.deserialize(inputBuffer, SubscribeAckMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof SubscribeAckMessage, is(true));
    }

    @Test
    public void testMessageDeserializerSubscribeFailedMessage() throws Exception {
        // act
        Message message = messageDeserializer.deserialize(inputBuffer, SubscribeFailedMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof SubscribeFailedMessage, is(true));
    }

    @Test
    public void testMessageDeserializerDropMessage() throws Exception {
        // setup
        when(inputBuffer.readByte()).thenReturn((byte) 0);

        // act
        Message message = messageDeserializer.deserialize(inputBuffer, DropMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof DropMessage, is(true));
    }

    @Test
    public void testMessageDeserializerUnsubscribeMessage() throws Exception {
        // act
        Message message = messageDeserializer.deserialize(inputBuffer, UnsubscribeMessage.TYPE, 0, nodeHandle);

        // assert
        assertThat(message instanceof UnsubscribeMessage, is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMessageDeserializerUnknownType() throws Exception {
        // act
        messageDeserializer.deserialize(inputBuffer, (short) 55, 0, nodeHandle);
    }

    @Test
    public void testThatAnycastFailureFiresOffAnApplicationEvent() throws Exception {
        // setup
        rice.pastry.NodeHandle source = mock(rice.pastry.NodeHandle.class);
        when(topic.getId()).thenReturn(topicId);
        Message message = new AnycastMessage(source, topic, scribeContent);

        // act
        scribe.deliver(topicId, message);

        // assert
        verify(applicationContext).publishEvent(argThat(new ArgumentMatcher<AnycastFailureEvent>() {
            @Override
            public boolean matches(Object argument) {
                AnycastFailureEvent anycastFailureEvent = (AnycastFailureEvent) argument;
                assertThat(anycastFailureEvent.getTopicId(), equalTo(topicId));
                assertThat(anycastFailureEvent.getKoalaScribeContent(), equalTo(scribeContent));
                return true;
            }
        }));
    }
}

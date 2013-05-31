package com.bt.pi.core.scribe.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.scribe.Topic;
import rice.p2p.util.rawserialization.SimpleInputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.pastry.Id;

import com.bt.pi.core.entity.EntityMethod;

@RunWith(MockitoJUnitRunner.class)
public class WrappedScribeContentMessageTest {
    private static final String TRANSACTION_ID = "transactionid";
    private NodeHandle sourceHandle;
    private Topic topic;
    private String json;

    private WrappedScribeContentMessage wrappedScribeContentMessage;

    @Before
    public void setup() {
        sourceHandle = mock(NodeHandle.class);
        topic = new Topic(Id.build("123"));
        json = "json";
        wrappedScribeContentMessage = new WrappedScribeContentMessage(sourceHandle, topic, WrappedScribeContentMessageType.ANYCAST, EntityMethod.UPDATE, json, TRANSACTION_ID);
    }

    @Test
    public void testGetters() {
        // assert
        assertThat(wrappedScribeContentMessage.getJsonData(), equalTo(json));
        assertThat(wrappedScribeContentMessage.getTopic(), equalTo(topic));
        assertThat(wrappedScribeContentMessage.getSource(), equalTo(sourceHandle));
        assertThat(wrappedScribeContentMessage.getEntityMethod(), equalTo(EntityMethod.UPDATE));
        assertThat(wrappedScribeContentMessage.getTransactionUID(), equalTo(TRANSACTION_ID));
        assertThat(wrappedScribeContentMessage.getType(), equalTo((short) 101));
    }

    @Test
    public void shouldBeAbleToRoundTripThroughSerialization() throws Exception {
        // setup
        SimpleOutputBuffer outBuf = new SimpleOutputBuffer();
        wrappedScribeContentMessage.serializeThis(outBuf);

        // act
        InputBuffer inBuf = new SimpleInputBuffer(outBuf.getBytes());
        WrappedScribeContentMessage reverse = new WrappedScribeContentMessage();
        reverse.deserializeThis(inBuf);

        // assert
        assertEquals(reverse.getEntityMethod(), wrappedScribeContentMessage.getEntityMethod());
        assertEquals(0, inBuf.bytesRemaining());
    }

    @Test
    public void shouldWriteExpectedNumberOfBitsWhenSerializing() throws Exception {
        // setup
        int expectedSize = 4 + WrappedScribeContentMessageType.ANYCAST.toString().getBytes("UTF-8").length + 4 + EntityMethod.UPDATE.toString().getBytes("UTF-8").length + 4 + json.getBytes("UTF-8").length + 4
                + TRANSACTION_ID.getBytes("UTF-8").length;

        SimpleOutputBuffer buf = new SimpleOutputBuffer();
        wrappedScribeContentMessage.serializeThis(buf);

        // assert
        assertEquals(expectedSize, buf.getBytes().length);
    }

}

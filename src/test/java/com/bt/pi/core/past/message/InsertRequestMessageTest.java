package com.bt.pi.core.past.message;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.pastry.Id;

import com.bt.pi.core.past.content.KoalaMutableContent;
import com.bt.pi.core.past.content.KoalaPiEntityContent;

public class InsertRequestMessageTest {

    private KoalaMutableContent content;
    private Id dest;
    private NodeHandle nodeHandle;
    private long expiration;

    @Before
    public void before() {
        content = new KoalaPiEntityContent(Id.build("foo"), "content", new HashMap<String, String>());
        dest = Id.build("dest");
        nodeHandle = mock(NodeHandle.class);
        expiration = System.currentTimeMillis();
    }

    @Test
    public void testGetType() throws Exception {
        // setup

        // act
        InsertRequestMessage message = new InsertRequestMessage(0, null, expiration, null, null);

        // assert
        assertThat(message.getType(), equalTo(InsertRequestMessage.TYPE));

    }

    @Test
    public void testExpiration() {
        // setup
        long expiration = System.currentTimeMillis();
        InsertRequestMessage message = new InsertRequestMessage(0, null, 0, null, null);

        // act
        message.setExpiration(expiration);

        assertThat(message.getExpiration(), equalTo(expiration));

    }

    @Test
    public void testToString() {
        // setup
        InsertRequestMessage message = new InsertRequestMessage(10198, content, expiration, nodeHandle, dest);

        System.err.println(message);
        // act & assert
        assertTrue(message.toString().contains(dest.toString()));
        assertTrue(message.toString().contains(nodeHandle.toString()));
        assertTrue(message.toString().contains("expiration=" + expiration));
        assertTrue(message.toString().contains("isResponse=false"));
        assertTrue(message.toString().contains("response=null"));
        assertTrue(message.toString().contains("exception=null"));
    }

    @Test
    public void testSerialize() throws Exception {
        // setup
        OutputBuffer outBuffer = mock(OutputBuffer.class);
        InsertRequestMessage message = new InsertRequestMessage(10198, content, expiration, nodeHandle, dest);

        // act
        message.serialize(outBuffer);

        // verify
        verify(outBuffer, atLeastOnce()).writeByte(eq((byte) 0));
        verify(outBuffer).writeLong(expiration);
    }
}

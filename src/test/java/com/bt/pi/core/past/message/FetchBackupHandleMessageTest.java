package com.bt.pi.core.past.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.past.rawserialization.PastContentHandleDeserializer;

public class FetchBackupHandleMessageTest {

    private NodeHandle nodeHandle;
    private Id idToFetch;
    private Id destId;
    private int messageNumber;
    private FetchBackupHandleMessage fetchBackupHandleMessage;

    @Before
    public void before() {
        nodeHandle = mock(NodeHandle.class);
        idToFetch = mock(Id.class);
        destId = mock(Id.class);
        messageNumber = 18;
        fetchBackupHandleMessage = new FetchBackupHandleMessage(messageNumber, idToFetch, nodeHandle, destId);
    }

    @Test
    public void testContructor() {

        // act
        FetchBackupHandleMessage fetchBackupHandleMes = new FetchBackupHandleMessage(messageNumber, idToFetch, nodeHandle, destId);

        // assert
        assertEquals(destId, fetchBackupHandleMes.getDestination());
        assertEquals(idToFetch, fetchBackupHandleMes.getId());
        assertEquals(nodeHandle, fetchBackupHandleMes.getSource());
        assertEquals(messageNumber, fetchBackupHandleMes.getUID());
    }

    @Test
    public void testStaticConstructor() throws Exception {
        // setup
        rice.p2p.commonapi.rawserialization.InputBuffer buf = mock(rice.p2p.commonapi.rawserialization.InputBuffer.class);
        when(buf.readByte()).thenReturn((byte) 0);
        short type = (short) 2;
        when(buf.readShort()).thenReturn(type);
        Endpoint endpoint = mock(Endpoint.class);
        PastContentHandleDeserializer pchd = mock(PastContentHandleDeserializer.class);

        // act
        FetchBackupHandleMessage.build(buf, endpoint, pchd);

        // assert
        verify(endpoint, atLeastOnce()).readId(eq(buf), eq(type));

    }

    @Test
    public void testToString() {
        // act
        String str = fetchBackupHandleMessage.toString();

        // assert
        assertTrue(str.contains(FetchBackupHandleMessage.class.getSimpleName()));
        assertTrue(str.contains(idToFetch.toString()));
    }

    @Test
    public void testSerialize() throws Exception {
        // setup
        OutputBuffer buf = mock(OutputBuffer.class);

        // act
        fetchBackupHandleMessage.serialize(buf);

        // assert
        verify(buf).writeShort(eq(idToFetch.getType()));
        verify(buf, atLeastOnce()).writeByte(eq((byte) 0));
    }
}

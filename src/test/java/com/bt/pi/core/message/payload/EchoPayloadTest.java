package com.bt.pi.core.message.payload;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.core.message.payload.EchoPayload;

public class EchoPayloadTest {

    @Test
    public void testGetType() {
        assertEquals("EchoPayload", new EchoPayload().getType());
    }
}

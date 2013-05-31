package com.bt.pi.core.dht;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.core.dht.DhtOperationTimeoutException;

public class DhtOperationTimeoutExceptionTest {

    @Test
    public void constructorTest() {
        String pooh = "pooh";
        DhtOperationTimeoutException koalaException = new DhtOperationTimeoutException(pooh);

        assertTrue(koalaException.getMessage().contains(pooh));
    }
}

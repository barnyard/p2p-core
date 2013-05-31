package com.bt.pi.core.exception;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PiConfigurationExceptionTest {

    @Test
    public void testMessageConstructor() {
        // act
        PiConfigurationException piConfigurationException = new PiConfigurationException("hello");

        // assert
        assertEquals("hello", piConfigurationException.getMessage());
    }

    @Test
    public void testExceptionConstructor() {
        // setup
        Exception e = new Exception();

        // act
        PiConfigurationException piConfigurationException = new PiConfigurationException(e);

        // assert
        assertEquals(e, piConfigurationException.getCause());
    }
}

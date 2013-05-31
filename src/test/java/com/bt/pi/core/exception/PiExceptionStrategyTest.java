package com.bt.pi.core.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.testing.LogHelper;
import com.bt.pi.core.testing.VectorAppender;

public class PiExceptionStrategyTest {

    private PiExceptionStrategy piExceptionStrategy;

    @Before
    public void before() {
        piExceptionStrategy = new PiExceptionStrategy();
    }

    @Test
    public void testHandleException() {
        // setup
        LogHelper.initLogging();

        // act
        piExceptionStrategy.handleException(this, new Exception("Selector don't die."));

        // assert
        assertTrue(VectorAppender.getMessages().get(0).contains("Handling exception in selector thread from com.bt.pi.core.exception.PiExceptionStrategyTest"));
    }
}

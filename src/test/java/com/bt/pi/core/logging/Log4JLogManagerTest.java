package com.bt.pi.core.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.logging.PiLog4JLogger;

import rice.environment.logging.Logger;

public class Log4JLogManagerTest {

    private Log4JLogManager logManager;

    @Before
    public void before() {
        logManager = new Log4JLogManager();
    }

    @Test
    public void shouldReturnPiLog4JLogger() {
        assertTrue(logManager.getLogger(this.getClass(), null) instanceof PiLog4JLogger);
    }

    @Test
    public void shouldReturnTheSameLoggerEachTime() {
        // setup
        Logger logger1 = logManager.getLogger(this.getClass(), "instance1");

        // act
        Logger logger2 = logManager.getLogger(this.getClass(), "instance1");

        // assert
        assertSame(logger1, logger2);
    }

    @Test
    public void shouldReturnDifferentLoggerForDifferentClass() {
        // setup
        Logger logger1 = logManager.getLogger(this.getClass(), "instance1");

        // act
        Logger logger2 = logManager.getLogger(String.class, "instance1");

        // assert
        assertNotSame(logger1, logger2);
    }

    @Test
    public void shouldReturnDifferentLoggerForDifferentInstance() {
        // setup
        Logger logger1 = logManager.getLogger(this.getClass(), "instance1");

        // act
        Logger logger2 = logManager.getLogger(this.getClass(), "instance2");

        // assert
        assertNotSame(logger1, logger2);
    }

    @Test
    public void shouldPassLogLeveltoPiLog4JLogger() {
        // setup
        logManager.setPastryLogLevel("trace");

        // assert
        assertEquals(PiLog4JLogger.FINEST, logManager.getLogger(getClass(), null).level);
    }
}

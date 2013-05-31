package com.bt.pi.core.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.core.testing.LogHelper;
import com.bt.pi.core.testing.VectorAppender;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PiLog4JLogger.class)
public class PiLog4JLoggerTest {
    private static final String LOG_MESSAGE = "boo YA";
    private static final String CLASS_INSTANCE = "hello";
    private PiLog4JLogger logger;

    @Before
    public void before() {
        logger = new PiLog4JLogger(this.getClass(), CLASS_INSTANCE, "fatal");
    }

    @Test
    public void testLogLevelConversionWithNull() {
        // act
        int result = logger.convertLog4JLevelToPastryLevel(null);

        // assert
        assertEquals(PiLog4JLogger.INFO, result);
    }

    @Test
    public void testLogLevelConversionWithBlank() {
        // act
        int result = logger.convertLog4JLevelToPastryLevel("");

        // assert
        assertEquals(PiLog4JLogger.INFO, result);
    }

    @Test
    public void testLogLevelConversionWithJunk() {
        // act
        int result = logger.convertLog4JLevelToPastryLevel("jfdsaopfjwepf");

        // assert
        assertEquals(PiLog4JLogger.INFO, result);
    }

    @Test
    public void testLogLevelConversion() {
        assertEquals(PiLog4JLogger.ALL, logger.convertLog4JLevelToPastryLevel("all"));
        assertEquals(PiLog4JLogger.FINEST, logger.convertLog4JLevelToPastryLevel("trace"));
        assertEquals(PiLog4JLogger.FINE, logger.convertLog4JLevelToPastryLevel("debug"));
        assertEquals(PiLog4JLogger.INFO, logger.convertLog4JLevelToPastryLevel("info"));
        assertEquals(PiLog4JLogger.WARNING, logger.convertLog4JLevelToPastryLevel("warn"));
        assertEquals(PiLog4JLogger.SEVERE, logger.convertLog4JLevelToPastryLevel("fatal"));
    }

    @Test
    public void testLogLevelSetProperlyFromConstructor() {
        assertEquals(PiLog4JLogger.FINEST, new PiLog4JLogger(this.getClass(), null, "trace").level);
    }

    @Test
    public void testLoggerCallsLogForJ() {
        // setup
        LogHelper.initLogging();

        // act
        logger.log(LOG_MESSAGE);

        // assert
        Vector<String> printouts = VectorAppender.getMessages();
        boolean pass = false;
        for (int i = 0; i < 100 && i < printouts.size() && !pass; i++) {
            if (printouts.get(i).contains(CLASS_INSTANCE) && printouts.get(i).contains(LOG_MESSAGE)) {
                pass = true;
            }
        }
        assertTrue("Unable to find class instance and log message.", pass);
    }

    @Test
    public void logShouldHandleSmallStacktraces() throws Exception {
        // setup
        Throwable t = Mockito.mock(Throwable.class);
        StackTraceElement[] stacktrace = new StackTraceElement[] { new StackTraceElement("a", "b", "c", 123) };
        when(t.getStackTrace()).thenReturn(stacktrace);
        PowerMockito.whenNew(Throwable.class).withNoArguments().thenReturn(t);

        // act
        this.logger.log("message");

        // assert
        // no exception
    }
}

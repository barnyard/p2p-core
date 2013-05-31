package com.bt.pi.core.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TimeBasedLoggerTest {

    private static final String HEADER = "header";
    private TimeBasedLogger<Void> voidLogger;

    private TimeBasedLogger<Boolean> booleanLogger;

    private TimeBasedLoggerConfiguration<Void> voidConf;

    private TimeBasedLoggerConfiguration<Boolean> booleanConf;

    @Mock
    private Log log;

    @Before
    public void before() {
        voidConf = new TimeBasedLoggerConfiguration<Void>();
        voidConf.setActive(true);
        voidConf.setLog(log);
        voidConf.setLogHeader(HEADER);
        booleanConf = new TimeBasedLoggerConfiguration<Boolean>();
        booleanConf.setActive(true);
        booleanConf.setLog(log);
        booleanConf.setLogHeader(HEADER);
        voidLogger = new TimeBasedLogger<Void>(voidConf);
        booleanLogger = new TimeBasedLogger<Boolean>(booleanConf);

    }

    @Test
    public void shouldLogWithVoidReturn() throws Exception {
        // setup
        voidConf.setCallableToRun(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Thread.sleep(200);
                return null;
            }
        });
        // act
        voidLogger.call();
        // assert
        verify(log).info(isA(String.class));
    }

    @Test
    public void shouldLogWithBooleanReturn() throws Exception {
        // setup
        booleanConf.setCallableToRun(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                Thread.sleep(200);
                return true;
            }
        });
        // act
        boolean result = booleanLogger.call();
        // assert
        assertTrue(result);
        verify(log).info(isA(String.class));

    }

    @Test
    public void shouldNotLogIfInactive() throws Exception {
        // setup
        voidConf.setActive(false);
        voidConf.setCallableToRun(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Thread.sleep(200);
                return null;
            }
        });
        // act
        voidLogger.call();
        // assert
        verify(log, never()).info(isA(String.class));

    }

    @Test
    public void shouldOnlyPrintOneLine() throws Exception {
        // setup
        voidConf.setCallableToRun(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                Thread.sleep(200);
                return null;
            }
        });
        // act
        voidLogger.call();
        voidLogger.call();
        // assert
        verify(log, times(1)).info(isA(String.class));

    }

}

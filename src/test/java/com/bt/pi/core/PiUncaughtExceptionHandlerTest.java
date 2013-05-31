package com.bt.pi.core;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.CoderMalfunctionError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { PiUncaughtExceptionHandler.class, LogFactory.class })
@PowerMockIgnore( { "org.apache.log4j.*" })
public class PiUncaughtExceptionHandlerTest {
    @InjectMocks
    private PiUncaughtExceptionHandler piUncaughtExceptionHandler = new PiUncaughtExceptionHandler();
    @Mock
    private Thread thread;
    @Mock
    private Throwable exception;
    @Mock
    private Log log;
    private String threadName = "mythread";

    @Before
    public void setup() {
        PowerMockito.mockStatic(LogFactory.class);
        PowerMockito.mockStatic(System.class);
        when(LogFactory.getLog(PiUncaughtExceptionHandler.class)).thenReturn(log);
        when(thread.getName()).thenReturn(threadName);

    }

    @Test
    public void testUncaughtException() {
        // setup

        // act
        piUncaughtExceptionHandler.uncaughtException(thread, exception);

        // assert
        verify(log).error("Exception in thread " + threadName, exception);
    }

    @Test
    public void testOnlyExitOnOutOfMemoryError() {
        // setup
        Error error = new OutOfMemoryError();

        // act
        piUncaughtExceptionHandler.uncaughtException(thread, error);

        // assert
        verify(log).error("Exception in thread " + threadName, error);
        verify(log).fatal("!!!!!!!!!!!!!!!!!!!  Error caught: " + error.getMessage() + ", exiting PI !!!!!!!!!!!!!!!!!!");
        PowerMockito.verifyStatic();
        System.exit(-1);

    }

    @Test
    public void shouldNotExitOnCoderMalfunctionErrorOrAnythingOtherThanVirtualMachineError() {
        // setup
        Error error = new CoderMalfunctionError(null);

        // act
        piUncaughtExceptionHandler.uncaughtException(thread, error);

        // assert
        verify(log).error("Exception in thread " + threadName, error);
        verify(log, never()).fatal("!!!!!!!!!!!!!!!!!!!  Error caught: " + error.getMessage() + ", exiting PI !!!!!!!!!!!!!!!!!!");
        PowerMockito.verifyZeroInteractions(System.class);
    }
}

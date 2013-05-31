package com.bt.pi.core.past.continuation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.continuation.ContinuationUtils;
import com.bt.pi.core.testing.UnitTestAppender;

public class LoggingContinuationWithKoalaHandleVersionComparerTest {
    private LoggingContinuationWithKoalaHandleVersionComparer<Object> loggingContinuationWithKoalaHandleVersionComparer;
    private static UnitTestAppender loggingAppender;

    @BeforeClass
    public static void beforeClass() {
        loggingAppender = new UnitTestAppender();
        Logger.getLogger(ContinuationUtils.class).addAppender(loggingAppender);

    }

    @Before
    public void setup() {
        loggingAppender.reset();
        loggingContinuationWithKoalaHandleVersionComparer = new LoggingContinuationWithKoalaHandleVersionComparer<Object>() {
            @Override
            protected void receiveExceptionInternal(Exception exception) {
            }

            @Override
            protected void receiveResultInternal(Object result) {
            }
        };
    }

    @Test
    public void testStartLogging() throws Exception {
        // act
        loggingContinuationWithKoalaHandleVersionComparer.receiveResult("123");

        // assert
        assertThat(loggingAppender.getMessages().contains("START: receiveResult(): null"), is(true));
        assertThat(loggingAppender.getMessages().get(1).startsWith("END: receiveResult(): null, DURATION:"), is(true));
    }

    @Test
    public void testEndLogging() throws Exception {
        // act
        loggingContinuationWithKoalaHandleVersionComparer.receiveException(new RuntimeException());

        // assert
        assertThat(loggingAppender.getMessages().contains("START: receiveException(): null"), is(true));
        assertThat(loggingAppender.getMessages().get(1).startsWith("END: receiveException(): null, DURATION:"), is(true));
    }
}

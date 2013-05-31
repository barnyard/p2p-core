package com.bt.pi.core.continuation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.testing.UnitTestAppender;

public class ContinuationUtilsTest {
    private static UnitTestAppender loggingAppender;

    @BeforeClass
    public static void beforeClass() {
        loggingAppender = new UnitTestAppender();
        Logger.getLogger(ContinuationUtils.class).addAppender(loggingAppender);

    }

    @Before
    public void setup() {
        loggingAppender.reset();
    }

    @Test
    public void testStartLogging() throws Exception {
        // act
        ContinuationUtils.logStartOfContinuation("methodName", "transactionUID");

        // assert
        assertThat(loggingAppender.getMessages().contains("START: methodName(): transactionUID"), is(true));
    }

    @Test
    public void testEndLogging() throws Exception {
        // act
        ContinuationUtils.logEndOfContinuation("methodName", "transactionUID", 0);

        // assert
        assertThat(loggingAppender.getMessages().get(0).startsWith("END: methodName(): transactionUID, DURATION:"), is(true));
    }
}

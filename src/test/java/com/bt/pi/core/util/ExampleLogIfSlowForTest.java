package com.bt.pi.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.stereotype.Component;

@Component
public class ExampleLogIfSlowForTest {
    private static Log LOG = LogFactory.getLog(ExampleLogIfSlowForTest.class);

    @Test
    public void dummyTest() {

    }

    @LogIfSlow(waitingTimeBetweenLogs = 500, timeOut = 100)
    public void testMethodOneArgument(String value1) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.interrupted();

        }

    }

    @LogIfSlow(waitingTimeBetweenLogs = 5000, timeOut = 100)
    public void testMethodNoArguments() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.interrupted();

        }
    }

    public void printSomething() {
        LOG.info("ExampleLogIfSlow printing something just to add some white noise");
    }

}

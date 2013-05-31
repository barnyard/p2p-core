package com.bt.pi.core.application.health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import rice.Continuation;

import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.testing.BeanPropertiesMunger;

public class LogMessagesTest extends HealthApplicationIntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(LogMessagesTest.class);

    @Rule
    public TestName testName = new TestName();

    @Before
    @Override
    public void setup() throws Exception {
        BeanPropertiesMunger.setDoMunging(true);
        BeanPropertiesMunger.setLogMessagePublishIntervalAndBroadcastSizes(5, 50);
        if (testName.getMethodName().equals("testThatTheLogMessageHandlersTruncateTheirLogQueues"))
            BeanPropertiesMunger.setLogMessageKeepCount(10);

        super.setup();

        setupErrorLogAppender();
    };

    @After
    public void resetProperties() {
        BeanPropertiesMunger.resetLogMessageKeepCount();
        BeanPropertiesMunger.resetLogMessagePublishIntervalAndBroadcastSizes();
        BeanPropertiesMunger.setDoMunging(false);
    }

    private void setupErrorLogAppender() throws Exception {
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOG.error("Error message");
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldGetAllLogMessages() throws Exception {
        LOG.debug("let some messages flow within pastry");
        Thread.sleep(60000);

        final CountDownLatch testLatch = new CountDownLatch(1);
        querySupernodeApplication(new LogMessageEntityCollection(), new Continuation<LogMessageEntityCollection, Exception>() {
            @Override
            public void receiveException(Exception arg0) {
                System.err.println("Exception in GET: " + arg0);
            }

            @Override
            public void receiveResult(LogMessageEntityCollection logMessageEntityCollection) {
                System.err.println("Received " + logMessageEntityCollection.getEntities().size() + " messages when looking for log messages");
                assertThat(logMessageEntityCollection.getEntities().size() > 20, is(true));
                testLatch.countDown();
            }
        });

        assertTrue(testLatch.await(3, TimeUnit.MINUTES));
    }

    @Test
    public void testThatTheLogMessageHandlersTruncateTheirLogQueues() throws Exception {
        Thread.sleep(30000);

        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);
            if (lordOfTheRings.getApplicationContextForNode(i).getBean(ReportingApplication.class).getApplicationStatus().equals(ApplicationStatus.ACTIVE)) {
                LogMessageHandler logMessageHandler = lordOfTheRings.getApplicationContextForNode(i).getBean(LogMessageHandler.class);
                assertThat(logMessageHandler.getAllEntities().getEntities().size() <= 10, is(true));
            }
        }
    }
}

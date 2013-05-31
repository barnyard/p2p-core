package com.bt.pi.core.application.health;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.application.activation.ApplicationStatus;
import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.testing.BeanPropertiesMunger;

public class HealthIntegrationTest extends HealthApplicationIntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(HealthIntegrationTest.class);
    private static final String ERROR_LOG_APPENDER = "errorLogAppender";
    private static final String NODE_PHYSICAL_HEALTH_CHECKER = "nodePhysicalHealthChecker";
    private static final String LOG_MESSAGE_HANDLER = "logMessageHandler";
    private static final String NODE_PHYSICAL_HEALTH_HANDLER = "nodePhysicalHealthHandler";

    private ScheduledExecutorService errorGeneratorThreadPool = Executors.newScheduledThreadPool(50);

    @After
    @Override
    public void after() {
        LOG.debug("after()");
    }

    @Before
    @Override
    public void setup() throws Exception {
        BeanPropertiesMunger.setDoMunging(true);
        BeanPropertiesMunger.setLogMessagePublishIntervalAndBroadcastSizes(120, 100);
        BeanPropertiesMunger.setLogMessageKeepCount(100);
        BeanPropertiesMunger.setHealthPublishIntervalAndBroadcastSizes(30, 50);

        LOG.debug("setup()");
        // super.setup();

        setupErrorLogAppender();
    };

    @After
    public void resetProperties() {
        BeanPropertiesMunger.resetHealthPublishIntervalAndBroadcastSizes();
        BeanPropertiesMunger.resetLogMessageKeepCount();
        BeanPropertiesMunger.resetLogMessagePublishIntervalAndBroadcastSizes();
        BeanPropertiesMunger.setDoMunging(false);
    }

    private void setupErrorLogAppender() throws Exception {
        errorGeneratorThreadPool.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                LOG.error("Error message");
            }
        }, 0, 750, TimeUnit.MILLISECONDS);
    }

    // BeforeClass and Before methods are commented out
    @Ignore("Really really big test, takes > 80 mins atm, but a good sanity check on the supernode stuff, via the health app")
    @Test
    public void shouldGetAllLogMessages() throws Exception {
        NodeStartedEvent nodeStartedEvent = new NodeStartedEvent(this);
        System.err.println(new Date());
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);

            ReportingApplication reportingApplication = (ReportingApplication) currentAppMap.get(ReportingApplication.APPLICATION_NAME);
            System.err.println(String.format("Node %d(%ssupernode) sent %d messages, received %d messages", i, reportingApplication.getApplicationStatus().equals(ApplicationStatus.ACTIVE) ? "" : "NOT ", reportingApplication.getMessagesSentCount(),
                    reportingApplication.getMessagesReceivedCount()));

            ((NodePhysicalHealthHandler) currentAppMap.get(NODE_PHYSICAL_HEALTH_HANDLER)).onApplicationEvent(nodeStartedEvent);
            ((NodePhysicalHealthChecker) currentAppMap.get(NODE_PHYSICAL_HEALTH_CHECKER)).onApplicationEvent(nodeStartedEvent);

            ((LogMessageHandler) currentAppMap.get(LOG_MESSAGE_HANDLER)).onApplicationEvent(nodeStartedEvent);
            ((ErrorLogAppender) currentAppMap.get(ERROR_LOG_APPENDER)).onApplicationEvent(nodeStartedEvent);
        }

        // let some messages flow within pastry
        // Thread.sleep(300 * 1000);

        final CountDownLatch testLatch = new CountDownLatch(160);

        final ReportingApplication myApplication = (ReportingApplication) currentAppMap.get(ReportingApplication.APPLICATION_NAME);

        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                PId id = koalaNode.getKoalaIdFactory().buildPIdFromHexString(superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(ReportingApplication.APPLICATION_NAME, region, zone));
                getLogMessageAndHeartbeatEntities(testLatch, myApplication, id);
            }
        }, 25, 60, TimeUnit.SECONDS);

        assertTrue(testLatch.await(90, TimeUnit.MINUTES));

        System.err.println(new Date());
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            ReportingApplication reportingApplication = (ReportingApplication) nodeApplicationsMap.get(i).get(ReportingApplication.APPLICATION_NAME);
            System.err.println(String.format("Node %d(%ssupernode) sent %d messages, received %d messages", i, reportingApplication.getApplicationStatus().equals(ApplicationStatus.ACTIVE) ? "" : "NOT ", reportingApplication.getMessagesSentCount(),
                    reportingApplication.getMessagesReceivedCount()));
        }
    }

    private void getLogMessageAndHeartbeatEntities(final CountDownLatch testLatch, ReportingApplication reportingApplication, PId id) {
        System.err.println(new Date());
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            ReportingApplication reportingApp = (ReportingApplication) nodeApplicationsMap.get(i).get(ReportingApplication.APPLICATION_NAME);
            System.err.println(String.format("Node %d(%ssupernode) sent %d messages, received %d messages", i, reportingApp.getApplicationStatus().equals(ApplicationStatus.ACTIVE) ? "" : "NOT ", reportingApp.getMessagesSentCount(), reportingApp
                    .getMessagesReceivedCount()));
        }

        System.err.println("Sending GET for Log messages");
        MessageContext messageContext = reportingApplication.newMessageContext();
        messageContext.routePiMessageToApplication(id, EntityMethod.GET, new LogMessageEntityCollection(), ReportingApplication.APPLICATION_NAME, new Continuation<LogMessageEntityCollection, Exception>() {
            @Override
            public void receiveException(Exception arg0) {
            }

            @Override
            public void receiveResult(LogMessageEntityCollection logMessageEntityCollection) {
                System.err.println("Received " + logMessageEntityCollection.getEntities().size() + " messages when looking for log messages");
                assertThat(logMessageEntityCollection.getEntities().size() > 20, is(true));
                for (LogMessageEntity logMessageEntity : logMessageEntityCollection.getEntities())
                    assertThat(logMessageEntity.getLogMessage(), equalTo("Error message"));

                testLatch.countDown();
            }
        });

        System.err.println("Sending GET for Heartbeats");
        messageContext = reportingApplication.newMessageContext();
        messageContext.routePiMessageToApplication(id, EntityMethod.GET, new HeartbeatEntityCollection(), ReportingApplication.APPLICATION_NAME, new Continuation<HeartbeatEntityCollection, Exception>() {
            @Override
            public void receiveException(Exception arg0) {
            }

            @Override
            public void receiveResult(HeartbeatEntityCollection heartbeatEntityCollection) {
                System.err.println("Received " + heartbeatEntityCollection.getEntities().size() + " messages when looking for heartbeats");
                for (HeartbeatEntity heartbeatEntity : heartbeatEntityCollection.getEntities()) {
                    assertNotNull(heartbeatEntity);

                    try {
                        assertEquals(InetAddress.getLocalHost().getHostName(), heartbeatEntity.getHostname());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                testLatch.countDown();
            }
        });
    }
}

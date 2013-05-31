package com.bt.pi.core.application.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.testing.BeanPropertiesMunger;

public class HeartbeatsTest extends HealthApplicationIntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(HeartbeatsTest.class);

    @Override
    public void setup() throws Exception {
        BeanPropertiesMunger.setDoMunging(true);
        BeanPropertiesMunger.setHealthPublishIntervalAndBroadcastSizes(4, 50);
        super.setup();
    };

    @After
    public void resetProperties() {
        BeanPropertiesMunger.resetHealthPublishIntervalAndBroadcastSizes();
        BeanPropertiesMunger.setDoMunging(false);
    }

    @Test
    public void shouldGetAllHeartbeats() throws Exception {
        LOG.debug("#### Running Test shouldGetAllHeartbeats ##### ");
        Thread.sleep(60000);

        final CountDownLatch testLatch = new CountDownLatch(1);

        querySupernodeApplication(new HeartbeatEntityCollection(), new Continuation<HeartbeatEntityCollection, Exception>() {
            @Override
            public void receiveException(Exception arg0) {
                System.err.println("EXCEPTION :" + arg0);
                arg0.printStackTrace();
            }

            @Override
            public void receiveResult(HeartbeatEntityCollection heartbeatEntityCollection) {
                System.err.println("Received " + heartbeatEntityCollection != null ? heartbeatEntityCollection.getEntities().size() : heartbeatEntityCollection + " messages when looking for heartbeats");
                assertTrue(heartbeatEntityCollection.getEntities().size() > 0);
                assertTrue(heartbeatEntityCollection.getEntities().size() > 0);
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

        assertTrue(testLatch.await(45, TimeUnit.SECONDS));
    }
}

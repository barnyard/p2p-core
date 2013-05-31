package com.bt.pi.core.past;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.UpdateAwareDhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class DhtOptimisticConcurrencyIntegrationTest extends RingHelper {
    private static final Log LOG = LogFactory.getLog(DhtOptimisticConcurrencyIntegrationTest.class);
    private static Properties properties;
    int replicas = 3;
    private int numNodes;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(DhtOptimisticConcurrencyIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void before() throws Exception {
        numNodes = 5;
        super.before(numNodes, properties);

        // createPastImpls();
        Thread.sleep(30000);
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Test
    public void singleTimedWrite() throws Exception {
        PId pastContentId = new PiId(rice.pastry.Id.build("timedTest" + System.currentTimeMillis()).toStringFull(), 0);
        ApplicationRecord applicationRecord = new GlobalScopedApplicationRecord("TimedWriteTest", 0);

        DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        long start = System.currentTimeMillis();
        blockingDhtWriter.put(pastContentId, applicationRecord);
        System.err.println("Time to write: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        applicationRecord.setApplicationName("updated");
        blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.update(pastContentId, applicationRecord, new UpdateResolver<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                existingEntity.setApplicationName("UPDATED!!");
                return existingEntity;
            }
        });
        System.err.println("Time to update: " + (System.currentTimeMillis() - start));

    }

    @Test
    public void writeLikeAZealot() throws Exception {
        LOG.info("###### Starting test Write Like a Zealot #########");
        // setup
        PId pastContentId = new PiId(rice.pastry.Id.build("dropTheHammer" + System.currentTimeMillis()).toStringFull(), 0);
        ApplicationRecord applicationRecord = new GlobalScopedApplicationRecord("writeLikeAZealot", 0);
        int numberOfWrites = 5;
        DhtClientFactory dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        BlockingDhtWriter blockingDhtWriter = dhtClientFactory.createBlockingWriter();
        blockingDhtWriter.put(pastContentId, applicationRecord);

        // act
        final CountDownLatch nodesLeftToUpdate = new CountDownLatch(lordOfTheRings.getRingSize() * numberOfWrites);
        for (int i = 0; i < lordOfTheRings.getRingSize() * numberOfWrites; i++) {
            final Integer writeNumber = new Integer(i);
            final Integer currentNodeIndex = i % lordOfTheRings.getRingSize();
            changeContextToNode(currentNodeIndex);

            dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(currentNodeIndex);
            DhtWriter dhtWriter = dhtClientFactory.createWriter();
            ((UpdateAwareDhtWriter) dhtWriter).setMaxNumVersionMismatchRetries(10);
            dhtWriter.update(pastContentId, null, new UpdateResolvingPiContinuation<ApplicationRecord>() {
                @Override
                public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                    return existingEntity;
                }

                @Override
                public void handleResult(ApplicationRecord result) {
                    System.err.println(String.format("Write # %d from node %d wrote version %d", writeNumber, currentNodeIndex, result.getVersion()));
                    nodesLeftToUpdate.countDown();
                }
            });
        }

        // assert
        assertTrue(nodesLeftToUpdate.await(360, TimeUnit.SECONDS));

        dhtClientFactory = lordOfTheRings.getDhtClientFactoryForNode(0);
        assertEquals(1 + lordOfTheRings.getRingSize() * numberOfWrites, dhtClientFactory.createBlockingReader().get(pastContentId).getVersion());
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        // TODO Auto-generated method stub
    }
}

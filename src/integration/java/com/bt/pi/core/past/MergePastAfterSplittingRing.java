package com.bt.pi.core.past;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.PId;

public class MergePastAfterSplittingRing extends DhtIntegrationTestBase {
    private static final Log LOG = LogFactory.getLog(MergePastAfterSplittingRing.class);
    private PId[] myPiEntityIds;
    private AtomicInteger count;

    @Before
    public void before() throws Exception {
        numberOfNodes = 5;
        super.before();

        count = new AtomicInteger(0);
    }

    @Override
    protected void seedInitialDhtRecords() {
        myPiEntityIds = new PId[lordOfTheRings.getRingSize()];
        for (int i = 0; i < myPiEntityIds.length; i++) {
            MyDhtPiEntity myPiEntity = new MyDhtPiEntity(String.format("[%d] Let's play ball: ", i));
            myPiEntityIds[i] = currentKoalaIdFactory.buildPId(myPiEntity.getUrl()).forLocalAvailabilityZone();
            currentDhtClientFactory.createBlockingWriter().put(myPiEntityIds[i], myPiEntity);
        }
    }

    private void readEntity(final int nodeNumber) {
        changeContextToNode(nodeNumber);

        DhtReader dhtReader = currentDhtClientFactory.createReader();
        dhtReader.getAsync(myPiEntityIds[nodeNumber], new PiContinuation<MyDhtPiEntity>() {
            @Override
            public void handleResult(MyDhtPiEntity result) {
                LOG.debug("Read result" + result);
                if (count.incrementAndGet() % 10 == 0)
                    throw new NullPointerException();
            }

            @Override
            public void handleException(Exception e) {
                LOG.error("Unable to read my pi entity from node " + nodeNumber, e);
            }
        });
    }

    private void updateEntityInNode(int nodeNumber, final String name) throws Exception {
        changeContextToNode(nodeNumber);

        final CountDownLatch latch = new CountDownLatch(1);
        DhtWriter dhtWriter = currentDhtClientFactory.createWriter();
        dhtWriter.update(myPiEntityIds[nodeNumber], new UpdateResolvingPiContinuation<MyDhtPiEntity>() {
            @Override
            public void handleResult(MyDhtPiEntity result) {
                LOG.info("Updated my pi entity to " + result);
                latch.countDown();
                if (count.incrementAndGet() % 10 == 0)
                    throw new NullPointerException();
            }

            @Override
            public MyDhtPiEntity update(MyDhtPiEntity existingEntity, MyDhtPiEntity requestedEntity) {
                if (existingEntity == null)
                    return new MyDhtPiEntity(name);

                existingEntity.setName(existingEntity.getName() + " " + name);
                return existingEntity;
            }
        });

        // // wait for dht update to complete
        // for (int i = 0; i < 500 && !latch.await(50, TimeUnit.MILLISECONDS); i++)
        // ;
        //
        // LOG.info(String.format("Result of dht update on node %d to name %s was %s", nodeNumber, name, latch.await(1,
        // TimeUnit.NANOSECONDS) ? "successful" : "unsuccessful"));
    }

    @Ignore("The test might fail due to the occasional exception")
    @Test
    public void testSplittingOfRingAndMergingOfData() throws Exception {
        takeNodesDown(2);
        LOG.info("Nodes 0 and 1 have gone down");

        updateEntityInNode(4, "strike 1,");
        LOG.info("Strike 1");
        updateEntityInNode(4, "strike 2,");
        LOG.info("Strike 2");
        updateEntityInNode(4, "strike 3, out.");
        LOG.info("1st Out");

        bringUpNodes(2, true);
        LOG.info("Nodes 1 and 2 have come back up in their own ring");

        updateEntityInNode(0, "ball 1,");
        LOG.info("Ball 1");
        updateEntityInNode(0, "ball 2,");
        LOG.info("Ball 2");
        updateEntityInNode(0, "ball 3,");
        LOG.info("Ball 3");
        updateEntityInNode(0, "ball 4, walk.");
        LOG.info("Walk");

        updateEntityInNode(4, "strike 1,");
        LOG.info("Strike 1");
        updateEntityInNode(4, "double, runners on 2nd & 3rd.");
        LOG.info("Double, 2nd and 3rd base");

        takeNodesDown(2);
        LOG.info("Nodes 0 and 1 have gone down again");

        bringUpNodes(2, false);
        LOG.info("Nodes 1 and 2 have come back up in the original ring");

        updateEntityInNode(4, "ground ball to 2B, out 2.");
        LOG.info("Out 2");

        updateEntityInNode(0, "fly ball to left field, inning over.");
        LOG.info("Inning over");

        Thread.sleep(30 * 1000);

        for (int i = 0; i < myPiEntityIds.length; i++) {
            MyDhtPiEntity result = (MyDhtPiEntity) currentDhtClientFactory.createBlockingReader().get(myPiEntityIds[i]);
            LOG.info("Final value in the dht is : " + result);
        }
    }

    @Ignore("The test might fail due to the occasional exception")
    @Test
    public void testSplittingOfRingAndCausingNetworkQueueExceptions() throws Exception {
        takeNodesDown(2);
        LOG.info("Nodes 0 and 1 have gone down");

        for (int i = 0; i < 20; i++) {
            updateEntityInNode(2, "strike 1,");
            updateEntityInNode(3, "strike 2,");
            updateEntityInNode(4, "strike 3, out.");
        }
        Thread.sleep(100 * 1000);
        LOG.info("Round 1 over");

        for (int i = 2; i < lordOfTheRings.getRingSize(); i++) {
            readEntity(i);
        }
        LOG.info("Round 1.5 over");

        bringUpNodes(2, true);
        LOG.info("Nodes 1 and 2 have come back up in their own ring");

        for (int i = 0; i < 20; i++) {
            updateEntityInNode(0, "ball 1,");
            updateEntityInNode(1, "ball 2,");
            updateEntityInNode(2, "strike 1,");
            updateEntityInNode(3, "strike 2,");
            updateEntityInNode(4, "strike 3, out.");
        }
        LOG.info("Round 2 over");

        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            readEntity(i);
        }
        LOG.info("Round 2.5 over");

        takeNodesDown(2);
        LOG.info("Nodes 0 and 1 have gone down again");

        bringUpNodes(2, false);
        LOG.info("Nodes 1 and 2 have come back up in the original ring");

        for (int i = 0; i < 20; i++) {
            updateEntityInNode(0, "ball 1,");
            updateEntityInNode(1, "ball 2,");
            updateEntityInNode(2, "strike 1,");
            updateEntityInNode(3, "strike 2,");
            updateEntityInNode(4, "strike 3, out.");
        }
        LOG.info("Round 3 over");

        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            readEntity(i);
        }
        LOG.info("Round 3.5 over");

        Thread.sleep(30 * 1000);

        for (int i = 0; i < myPiEntityIds.length; i++) {
            MyDhtPiEntity result = (MyDhtPiEntity) currentDhtClientFactory.createBlockingReader().get(myPiEntityIds[i]);
            LOG.info("Final value in the dht is : " + result);
        }
    }
}

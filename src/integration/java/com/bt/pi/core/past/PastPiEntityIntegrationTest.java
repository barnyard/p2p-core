package com.bt.pi.core.past;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rice.Continuation;
import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class PastPiEntityIntegrationTest extends RingHelper {
    private static final int WAIT_TIMEOUT_SECS = 280;
    private static final Log LOG = LogFactory.getLog(PastPiEntityIntegrationTest.class);
    private static Properties properties;

    Continuation<Object, Exception> lookupHandlesContinuation;
    Exception lookupException;
    Object lookupResult;
    Continuation<PiEntity, Exception> fetchContinuation;
    Exception fetchException;
    Object fetchresult;
    String testData;
    Continuation<Boolean[], Exception> insertContinuation;
    Exception insertException;
    Boolean[] insertResult;
    Semaphore fetchSemaphore, insertSemaphore, lookupSemaphore;
    private Id id;

    @BeforeClass
    public static void beforeClass() throws Exception {
        properties = new Properties();
        properties.load(PastPiEntityIntegrationTest.class.getClassLoader().getResourceAsStream(P2P_INTEGRATION_PROPERTIES));
        BeanPropertiesMunger.setDoMunging(true);
    }

    @Before
    public void testBefore() throws Exception {
        super.beforeOnlyOnce(5, properties);
        insertException = null;
        insertResult = null;
        insertSemaphore = new Semaphore(0);
        insertContinuation = new Continuation<Boolean[], Exception>() {

            @Override
            public void receiveException(Exception exception) {
                insertException = exception;
                LOG.error("Insert Exception: " + insertException.getMessage() + ": " + insertException);
                insertSemaphore.release();
            }

            @Override
            public void receiveResult(Boolean[] result) {
                insertResult = result;
                LOG.info("Insert result calledBack: " + insertResult);
                insertSemaphore.release();
            }
        };

        fetchException = null;
        fetchresult = null;
        fetchSemaphore = new Semaphore(0);
        fetchContinuation = new Continuation<PiEntity, Exception>() {

            @Override
            public void receiveException(Exception exception) {
                fetchException = exception;
                LOG.error("Fetch Exception: " + fetchException.getMessage() + ": " + fetchException);
                fetchSemaphore.release();
            }

            @Override
            public void receiveResult(PiEntity result) {
                fetchresult = result;
                LOG.info("Fetch result calledBack: " + fetchresult);
                fetchSemaphore.release();
            }

        };
    }

    @AfterClass
    public static void cleanup() throws Exception {
        afterClass();
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        // TODO Auto-generated method stub
    }

    @Test
    public void testWrite() throws Exception {
        // setup
        changeContextToNode(0);
        PId pid = new PiId(rice.pastry.Id.build("The one serious conviction that a man should have is that nothing is to be taken too seriously.").toStringFull(), 0);

        // act
        currentStorage.put(pid, new EchoPayload(), insertContinuation);

        // wait
        waitForInsertContinuation();

        // assert
        assertNull(insertException);
        assertEquals(koalaNode.getPast().getReplicationFactor() + 1, insertResult.length);
        for (int i = 0; i < insertResult.length; i++) {
            assertTrue(insertResult[i]);
        }
    }

    @Test
    public void testWriteAndRead() throws Exception {
        // setup
        changeContextToNode(0);
        PId pid = new PiId(rice.pastry.Id.build("coolBeans").toStringFull(), 0);

        ApplicationRecord appRecord = new GlobalScopedApplicationRecord("testWriteAndRead", 0);
        currentStorage.put(pid, appRecord, insertContinuation);
        waitForInsertContinuation();

        // act
        currentStorage.getAny(pid, fetchContinuation);

        // wait
        waitForFetchContinuation();

        // assert
        assertTrue(fetchresult instanceof ApplicationRecord);
        assertEquals(((ApplicationRecord) fetchresult).getVersion(), appRecord.getVersion());
        assertEquals(((ApplicationRecord) fetchresult).getUrl(), appRecord.getUrl());
        assertEquals(((ApplicationRecord) fetchresult).getApplicationName(), appRecord.getApplicationName());
    }

    @Test
    public void testWriteAndReadBackups() throws Exception {
        // setup
        changeContextToNode(0);
        PId pastContentId = currentStorage.getKoalaIdFactory().buildPId("testWriteAndReadBackups - Don't try to solve serious matters in the middle of the night.");
        final EchoPayload mypayload = new EchoPayload();
        currentStorage.put(pastContentId, mypayload, insertContinuation);
        // wait
        waitForInsertContinuation();
        int numberOfBackups = 2;
        Set<String> backUpIds = currentStorage.generateBackupIds(numberOfBackups, NodeScope.REGION, pastContentId);
        System.err.println("Backup Ids for lookup: " + backUpIds);
        // act
        final CountDownLatch latch = new CountDownLatch(backUpIds.size());
        waitForBackups(mypayload, backUpIds, latch, 4);

        // assert
        assertEquals(numberOfBackups, backUpIds.size());
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }

    public void waitForBackups(final EchoPayload mypayload, Set<String> backUpIds, final CountDownLatch latch, final int retries) {
        System.err.println(String.format("waitForBackups(payload -%s, backupIds - %s, latch - %s, retriesleft - %s", mypayload, backUpIds, latch, retries));
        for (final String idStr : backUpIds) {
            PId id = new PiId(idStr, 0).forDht();
            System.err.println("calling getAnyfor Id: " + id.toStringFull());
            currentStorage.getAny(id, new PiContinuation<EchoPayload>() {
                @Override
                public void handleResult(EchoPayload result) {
                    if (result != null) {
                        latch.countDown();
                        System.err.println("backup present: " + result);
                        assertEquals(mypayload, result);
                    } else if (retries > 0) {
                        TreeSet<String> retrySet = new TreeSet<String>();
                        retrySet.add(idStr);
                        waitForBackups(mypayload, retrySet, latch, retries - 1);
                    }
                }

                @Override
                public void handleException(Exception e) {
                    latch.countDown();
                    fail(e.toString());
                }
            });
        }
    }

    @Test
    public void testWriteAndGetWithBackupFailOver() throws Exception {
        // setup
        changeContextToNode(1);
        String nodeId = currentPastryNode.getNodeId().toStringFull();
        // We are creating the Id in this very disgusting way so that node 1 will be the daddy.
        // final String pastIdStr = nodeId.substring(0, 36) +
        // StringUtils.leftPad(Integer.toHexString(currentApplicationContext.getBean(KoalaPiEntityFactory.class).getCodeForEntity(EchoPayload.class)
        // << 1), 4, '0');
        final String pastIdStr = nodeId.substring(0, 36) + "0022";
        PId pastContentId = new PiId(pastIdStr, 0);
        final EchoPayload mypayload = new EchoPayload();
        currentStorage.put(pastContentId, mypayload, insertContinuation);
        // wait
        waitForInsertContinuation();
        changeContextToNode(0);
        Set<String> backUpIds = currentStorage.generateBackupIds(2, NodeScope.REGION, pastContentId);
        final CountDownLatch latch = new CountDownLatch(backUpIds.size());
        waitForBackups(mypayload, backUpIds, latch, 5);
        latch.await(30, TimeUnit.SECONDS);
        // remove record from our store.
        currentPersistentStorage.unstore(currentStorage.getKoalaIdFactory().buildId(pastContentId.getIdAsHex()), new Continuation<Object, Exception>() {

            @Override
            public void receiveException(Exception exception) {
                LOG.debug("unstore exception: " + exception);
                fail("Dude... I dunno. We couldn't delete the record in past.");
            }

            @Override
            public void receiveResult(Object result) {
                LOG.debug("unstore result: " + result);
            }
        });

        // act
        currentStorage.get(pastContentId, fetchContinuation);

        // wait
        waitForFetchContinuation();

        System.err.println("result " + fetchresult);
        System.err.println("exception " + fetchException);
        System.err.println("BackupIds: " + ArrayUtils.toString(backUpIds));
        assertEquals(mypayload, fetchresult);
    }

    @Test
    public void testWriteAndReadWithVersionedEntity() throws Exception {
        // setup
        changeContextToNode(0);
        ApplicationRecord app = new GlobalScopedApplicationRecord("testWriteAndReadWithVersionedEntity", 0);
        PId pid = new PiId(rice.pastry.Id.build(app.getUrl()).toStringFull(), 0);
        currentStorage.put(pid, app, insertContinuation);
        waitForInsertContinuation();

        // act
        currentStorage.getAny(pid, fetchContinuation);

        // wait
        waitForFetchContinuation();

        LOG.info(fetchresult);
        assertEquals(app, fetchresult);
    }

    @Test
    public void testWriteAndReadLatestVersionedEntity() throws Exception {
        // setup
        changeContextToNode(0);
        ApplicationRecord app = new GlobalScopedApplicationRecord("testWriteAndReadLatestVersionedEntity", 0);
        PId pid = new PiId(rice.pastry.Id.build(app.getUrl()).toStringFull(), 0);
        currentStorage.put(pid, app, insertContinuation);
        waitForInsertContinuation();

        // act
        currentStorage.get(pid, fetchContinuation);

        // wait
        waitForFetchContinuation();

        // assert
        LOG.info(fetchresult);
        assertEquals(app, fetchresult);
    }

    @Test
    public void testWriteAndGetAnyEntity() throws Exception {
        // setup
        changeContextToNode(0);
        ApplicationRecord appRecord = new GlobalScopedApplicationRecord("testWriteAndGetAnyEntity", 0);
        PId pid = new PiId(rice.pastry.Id.build(appRecord.getUrl()).toStringFull(), 0);
        currentStorage.put(pid, appRecord, insertContinuation);
        waitForInsertContinuation();

        // act
        currentStorage.getAny(pid, fetchContinuation);

        // wait
        waitForFetchContinuation();

        // assert
        LOG.info(fetchresult);
        assertEquals(appRecord, fetchresult);
    }

    @Test
    public void testCompareGetAndGetAnyTestTimes() throws Exception {
        // setup
        // insert some data
        changeContextToNode(0);
        ApplicationRecord appRecord = new GlobalScopedApplicationRecord("testCompareGetAndGetAnyTestTimes", 0);
        PId pid = new PiId(rice.pastry.Id.build(appRecord.getUrl()).toStringFull(), 0);
        currentStorage.put(pid, appRecord, insertContinuation);
        waitForInsertContinuation();

        long getAnystartTime = System.currentTimeMillis();
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);
            currentStorage.getAny(pid, fetchContinuation);
            // wait
            waitForFetchContinuation();
            assertEquals(appRecord, fetchresult);
            fetchresult = null;
            fetchException = null;
        }
        long getAnyFinish = System.currentTimeMillis();

        // try reading with get
        long getstartTime = System.currentTimeMillis();
        long getfinish;
        for (int i = 0; i < lordOfTheRings.getRingSize(); i++) {
            changeContextToNode(i);
            currentStorage.get(pid, fetchContinuation);
            // wait
            waitForFetchContinuation();
            assertEquals(appRecord, fetchresult);
            fetchresult = null;
            fetchException = null;
        }
        getfinish = System.currentTimeMillis();

        LOG.error("Storage get took:" + (getfinish - getstartTime) + " miliseconds total. Average: " + ((getfinish - getstartTime) / lordOfTheRings.getRingSize()));
        LOG.error("Storage getAny took:" + (getAnyFinish - getAnystartTime) + " miliseconds total. Average: " + ((getAnyFinish - getAnystartTime) / lordOfTheRings.getRingSize()));
    }

    private void waitForInsertContinuation() throws Exception {
        LOG.info("Waiting for insert.");
        insertSemaphore.tryAcquire(WAIT_TIMEOUT_SECS, TimeUnit.SECONDS);
    }

    private void waitForFetchContinuation() throws Exception {
        LOG.info("Waiting for insert.");
        fetchSemaphore.tryAcquire(WAIT_TIMEOUT_SECS, TimeUnit.SECONDS);
    }
}

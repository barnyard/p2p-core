package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import rice.p2p.commonapi.Id;
import rice.p2p.past.gc.GCPastMetadata;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.pastry_override.PersistentStorage;

public class LocalStorageMetaDataTest {
    private static ClassPathXmlApplicationContext classPathXmlApplicationContext;
    private static KoalaIdFactory koalaIdFactory;
    private static DhtClientFactory dhtClientFactory;
    private static KoalaNode koalaNode;
    private static int metaDataSyncTime = 1000;
    private static String nodeId;
    private PiEntity piEntity;
    private PId id;

    @BeforeClass
    public static void beforeClass() throws Exception {
        startContext();
        nodeId = FileUtils.readFileToString(new File("var/run/nodeId.txt"));
    }

    private static void startContext() throws Exception {
        classPathXmlApplicationContext = new ClassPathXmlApplicationContext("applicationContext-p2p-core-integration.xml");
        koalaNode = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
        koalaIdFactory = (KoalaIdFactory) classPathXmlApplicationContext.getBean("koalaIdFactory");
        koalaIdFactory.setAvailabilityZone(45);
        koalaIdFactory.setRegion(56);
        dhtClientFactory = (DhtClientFactory) classPathXmlApplicationContext.getBean("dhtClientFactory");
        koalaNode.setStorageMetadataSyncTime(metaDataSyncTime);
        koalaNode.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        stopContext();
        FileUtils.deleteQuietly(new File("storage" + nodeId));
        FileUtils.deleteQuietly(new File("var"));
    }

    private static void stopContext() throws Exception {
        koalaNode.stop();
        classPathXmlApplicationContext.destroy();
    }

    @Before
    public void before() {
        piEntity = new LocalStorageMetaDataTestPiEntity();
        id = koalaIdFactory.buildPId(piEntity.getUrl());
        BlockingDhtWriter blockingWriter = dhtClientFactory.createBlockingWriter();
        blockingWriter.writeIfAbsent(id, piEntity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMetaDataIsInMemory() {

        // act
        SortedMap<Id, GCPastMetadata> result = koalaNode.getPersistentDhtStorage().scanMetadata();

        // assert
        assertEquals(1, result.size());
        assertEquals(id.toStringFull(), result.firstKey().toStringFull());
        KoalaGCPastMetadata koalaContentMetadata = (KoalaGCPastMetadata) result.get(koalaNode.getKoalaIdFactory().buildId(id));
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertEquals(true, koalaContentMetadata.isDeletedAndDeletable());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatMetadataGetsPersisted() throws Exception {
        // setup

        // act
        allowNodeTimeToPersistMetadata();
        // re-start app
        stopContext();
        String cacheFileName = "./storage" + nodeId + PersistentStorage.BACKUP_DIRECTORY + KoalaNode.KOALA_DISK_STORAGE_NAME + "/" + PersistentStorage.METADATA_FILENAME;
        assertTrue(new File(cacheFileName).exists());
        startContext();

        // assert
        SortedMap<Id, GCPastMetadata> result = koalaNode.getPersistentDhtStorage().scanMetadata();

        assertTrue(result.size() > 0);
        KoalaGCPastMetadata koalaContentMetadata = (KoalaGCPastMetadata) result.get(koalaNode.getKoalaIdFactory().buildId(id));
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertEquals(true, koalaContentMetadata.isDeletedAndDeletable());
    }

    private void allowNodeTimeToPersistMetadata() throws Exception {
        Thread.sleep(2 * metaDataSyncTime);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testThatMetadataIsRebuiltFromActualDataIfCacheFileIsMissing() throws Exception {
        // setup
        allowNodeTimeToPersistMetadata();

        // act
        stopContext();

        // delete cache file
        String cacheFileName = "./storage" + nodeId + PersistentStorage.BACKUP_DIRECTORY + KoalaNode.KOALA_DISK_STORAGE_NAME + "/" + PersistentStorage.METADATA_FILENAME;
        assertTrue(new File(cacheFileName).exists());
        assertTrue(FileUtils.deleteQuietly(new File(cacheFileName)));

        startContext();

        // assert
        SortedMap<Id, GCPastMetadata> result = koalaNode.getPersistentDhtStorage().scanMetadata();

        assertTrue(result.size() > 0);
        Id idToCheck = koalaNode.getKoalaIdFactory().buildId(id);
        assertTrue(result.containsKey(idToCheck));
        KoalaGCPastMetadata koalaContentMetadata = (KoalaGCPastMetadata) result.get(koalaNode.getKoalaIdFactory().buildId(id));
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertEquals(true, koalaContentMetadata.isDeletedAndDeletable());
    }

    @Test
    public void testThatMetaDataFileIsUpdatedWhenDataIsUpdated() throws Exception {
        // setup
        final CountDownLatch latch = new CountDownLatch(1);

        allowNodeTimeToPersistMetadata();
        GCPastMetadata koalaContentMetadata = readMetadataFromDisk(id);
        assertEquals(1, koalaContentMetadata.getExpiration());

        koalaContentMetadata = readMetadataFromMemory(id);
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertEquals(true, ((KoalaGCPastMetadata) koalaContentMetadata).isDeletedAndDeletable());

        // act
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(id, new UpdateResolvingContinuation<LocalStorageMetaDataTestPiEntity, Exception>() {

            @Override
            public LocalStorageMetaDataTestPiEntity update(LocalStorageMetaDataTestPiEntity existingEntity, LocalStorageMetaDataTestPiEntity requestedEntity) {
                existingEntity.setStuff("stuff");
                existingEntity.setDeleted(true);
                return existingEntity;
            }

            @Override
            public void receiveException(Exception arg0) {
            }

            @Override
            public void receiveResult(LocalStorageMetaDataTestPiEntity arg0) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        allowNodeTimeToPersistMetadata();

        stopContext();

        // assert
        koalaContentMetadata = readMetadataFromDisk(id);
        assertEquals(2, koalaContentMetadata.getExpiration());
        assertEquals(true, ((KoalaGCPastMetadata) koalaContentMetadata).isDeletedAndDeletable());

        startContext(); // for next test
    }

    @SuppressWarnings("unchecked")
    private GCPastMetadata readMetadataFromMemory(PId id) {
        SortedMap<Id, GCPastMetadata> map = koalaNode.getPersistentDhtStorage().scanMetadata();
        return map.get(koalaIdFactory.buildId(id));
    }

    @SuppressWarnings("unchecked")
    private GCPastMetadata readMetadataFromDisk(PId id) throws Exception {
        String cacheFileName = "./storage" + nodeId + PersistentStorage.BACKUP_DIRECTORY + KoalaNode.KOALA_DISK_STORAGE_NAME + "/" + PersistentStorage.METADATA_FILENAME;
        ObjectInputStream objin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFileName)));
        HashMap map = (HashMap) objin.readObject();
        return (GCPastMetadata) map.get(koalaIdFactory.buildId(id));
    }

    public static class LocalStorageMetaDataTestPiEntity extends PiEntityBase implements Deletable {
        private String url;
        private String stuff;
        private boolean deleted;

        public LocalStorageMetaDataTestPiEntity() {
            url = getClass().getSimpleName() + ":" + System.currentTimeMillis();
            deleted = true;
        }

        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return url;
        }

        public void setStuff(String stuff) {
            this.stuff = stuff;
        }

        public String getStuff() {
            return stuff;
        }

        @Override
        public boolean isDeleted() {
            return this.deleted;
        }

        @Override
        public void setDeleted(boolean b) {
            this.deleted = b;
        }

        @Override
        public String getUriScheme() {
            return this.getClass().getName();
        }
    }
}

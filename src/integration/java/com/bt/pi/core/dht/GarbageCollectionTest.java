package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.core.dht.LocalStorageMetaDataTest.LocalStorageMetaDataTestPiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;

/* this test is too assert that the "GC" part of the GCPast is switched off */
public class GarbageCollectionTest {
    private static ClassPathXmlApplicationContext classPathXmlApplicationContext;
    private static KoalaIdFactory koalaIdFactory;
    private static DhtClientFactory dhtClientFactory;
    private static KoalaNode koalaNode;
    private static int metaDataSyncTime = 1000;
    private static String nodeId;
    private LocalStorageMetaDataTestPiEntity piEntity;
    private PId id;
    private String stuff = "stuff";
    private static int gCPastCollectionInterval = 1000;

    @BeforeClass
    public static void beforeClass() throws Exception {
        startContext();
        nodeId = FileUtils.readFileToString(new File("var/run/nodeId.txt"));
    }

    private static void startContext() throws Exception {
        classPathXmlApplicationContext = new ClassPathXmlApplicationContext("applicationContext-p2p-core-integration.xml");
        koalaNode = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
        koalaNode.setStorageMetadataSyncTime(metaDataSyncTime);
        koalaNode.setPastGCCollectionInterval(gCPastCollectionInterval);
        koalaNode.start();
        koalaIdFactory = (KoalaIdFactory) classPathXmlApplicationContext.getBean("koalaIdFactory");
        dhtClientFactory = (DhtClientFactory) classPathXmlApplicationContext.getBean("dhtClientFactory");
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
        piEntity.setStuff(stuff);
        id = koalaIdFactory.buildPId(piEntity.getUrl());
        BlockingDhtWriter blockingWriter = dhtClientFactory.createBlockingWriter();
        blockingWriter.writeIfAbsent(id, piEntity);
    }

    @Test
    public void testMetaDataIsStillThere() throws InterruptedException {

        // act
        Thread.sleep(10 * gCPastCollectionInterval);

        // assert
        LocalStorageMetaDataTestPiEntity result = (LocalStorageMetaDataTestPiEntity) dhtClientFactory.createBlockingReader().get(id);
        assertEquals(stuff, result.getStuff());
    }
}

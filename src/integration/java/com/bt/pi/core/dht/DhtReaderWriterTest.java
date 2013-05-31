package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;

public class DhtReaderWriterTest {
    private static ClassPathXmlApplicationContext classPathXmlApplicationContext;
    private static KoalaIdFactory koalaIdFactory;
    private static DhtClientFactory dhtClientFactory;
    private static KoalaNode koalaNode;

    @BeforeClass
    public static void beforeClass() throws Exception {
        classPathXmlApplicationContext = new ClassPathXmlApplicationContext("applicationContext-p2p-core-integration.xml");
        koalaNode = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
        koalaIdFactory = (KoalaIdFactory) classPathXmlApplicationContext.getBean("koalaIdFactory");
        dhtClientFactory = (DhtClientFactory) classPathXmlApplicationContext.getBean("dhtClientFactory");
        koalaNode.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        koalaNode.stop();
        classPathXmlApplicationContext.destroy();
        String nodeId = FileUtils.readFileToString(new File("var/run/nodeId.txt"));
        FileUtils.deleteQuietly(new File("storage" + nodeId));
        FileUtils.deleteQuietly(new File("var"));
    }

    @Test
    public void testWriteIfAbsent() {
        // setup
        PiEntity piEntity = new MyDhtRWPiEntity();
        PId id = koalaIdFactory.buildPId(piEntity.getUrl());
        BlockingDhtWriter blockingWriter = dhtClientFactory.createBlockingWriter();

        // act
        blockingWriter.writeIfAbsent(id, piEntity);

        // assert
        assertEquals(piEntity, blockingWriter.getValueWritten());
    }

    @Test
    public void testWriteIfAbsentTwice() {
        // setup
        PiEntity piEntity = new MyDhtRWPiEntity();
        PId id = koalaIdFactory.buildPId(piEntity.getUrl());

        // act
        BlockingDhtWriter blockingWriter1 = dhtClientFactory.createBlockingWriter();
        blockingWriter1.writeIfAbsent(id, piEntity);

        // assert
        assertEquals(piEntity, blockingWriter1.getValueWritten());

        // act
        BlockingDhtWriter blockingWriter2 = dhtClientFactory.createBlockingWriter();
        blockingWriter2.writeIfAbsent(id, piEntity);

        // assert
        assertNull(blockingWriter2.getValueWritten());
    }

    @Test
    public void testWriteIfAbsentTwiceOnDeletedDeletable() {
        // setup
        MyDeletablePiEntity piEntity = new MyDeletablePiEntity();
        piEntity.setDeleted(true);
        PId id = koalaIdFactory.buildPId(piEntity.getUrl());

        // act
        BlockingDhtWriter blockingWriter1 = dhtClientFactory.createBlockingWriter();
        blockingWriter1.writeIfAbsent(id, piEntity);

        // assert
        assertEquals(piEntity, blockingWriter1.getValueWritten());

        // act
        BlockingDhtWriter blockingWriter2 = dhtClientFactory.createBlockingWriter();
        piEntity.setDeleted(false);
        blockingWriter2.writeIfAbsent(id, piEntity);

        // assert
        assertEquals(piEntity, blockingWriter2.getValueWritten());
    }

    @Test
    public void shouldNotFailToUpdatePiEntityIfItExistsAndDeleted() {
        // setup
        // 1.
        System.err.println("Create a new Hello entity");
        final MyDeletablePiEntity piEntity0 = new MyDeletablePiEntity();
        PId id0 = koalaIdFactory.buildPId(piEntity0.getUrl());

        for (int i = 0; i < 20; i++) {
            dhtClientFactory.createBlockingWriter().update(id0, null, new UpdateResolver<MyDeletablePiEntity>() {

                @Override
                public MyDeletablePiEntity update(MyDeletablePiEntity existingEntity, MyDeletablePiEntity requestedEntity) {
                    piEntity0.setMessage("Hello:" + System.currentTimeMillis());
                    return piEntity0;
                }
            });
        }

        // 2.
        System.err.println("Deleting Hello entity");

        dhtClientFactory.createBlockingWriter().update(id0, null, new UpdateResolver<MyDeletablePiEntity>() {

            @Override
            public MyDeletablePiEntity update(MyDeletablePiEntity existingEntity, MyDeletablePiEntity requestedEntity) {
                existingEntity.setMessage("Hello Deleted");
                existingEntity.setDeleted(true);
                return existingEntity;
            }
        });

        // 3.
        System.err.println("Create another new Hello entity");
        // act
        MyDeletablePiEntity piEntity2 = new MyDeletablePiEntity();
        piEntity2.setDeleted(false);
        piEntity2.setMessage("World");

        BlockingDhtWriter blockingWriter2 = dhtClientFactory.createBlockingWriter();

        blockingWriter2.writeIfAbsent(id0, piEntity2);

        // assert
        assertEquals(piEntity2, blockingWriter2.getValueWritten());
    }

    @Test
    public void testWriteIfAbsentTwiceOnNotDeletedDeletable() {
        // setup
        MyDeletablePiEntity piEntity = new MyDeletablePiEntity();
        piEntity.setDeleted(false);
        PId id = koalaIdFactory.buildPId(piEntity.getUrl());

        // act
        BlockingDhtWriter blockingWriter1 = dhtClientFactory.createBlockingWriter();
        blockingWriter1.writeIfAbsent(id, piEntity);

        // assert
        assertEquals(piEntity, blockingWriter1.getValueWritten());

        // act
        BlockingDhtWriter blockingWriter2 = dhtClientFactory.createBlockingWriter();
        blockingWriter2.writeIfAbsent(id, piEntity);

        // assert
        assertNull(blockingWriter2.getValueWritten());
    }

    public static class MyDhtRWPiEntity extends PiEntityBase {
        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return getClass().getSimpleName() + ":" + System.currentTimeMillis();
        }

        @Override
        public String getUriScheme() {
            return "mdrwpe";
        }
    }

    public static class MyDeletablePiEntity extends PiEntityBase implements Deletable {
        private boolean deleted = false;
        private String url;
        private String message;

        public MyDeletablePiEntity() {
            this.url = getClass().getSimpleName() + ":" + System.currentTimeMillis();
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String aMessage) {
            message = aMessage;
        }

        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return url;
        }

        @Override
        public boolean isDeleted() {
            return deleted;
        }

        @Override
        public void setDeleted(boolean b) {
            this.deleted = b;
        }

        @Override
        public boolean equals(Object obj) {
            MyDeletablePiEntity other = (MyDeletablePiEntity) obj;
            return other.url.equals(url) && (other.deleted == deleted);
        }

        @Override
        public String toString() {
            return "MyDeletablePiEntity [deleted=" + deleted + ", message=" + message + ", url=" + url + ", version=" + this.getVersion() + "]";
        }

        @Override
        public String getUriScheme() {
            return "mdpe";
        }
    }
}

package com.bt.pi.core.application.reporter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class SizeBoundReportableEntityStoreTest {
    private static final String NODE_ID = "nodeId";
    private MyPiEntity piEntity1;
    private MyPiEntity piEntity2;
    private SizeBoundReportableEntityStore<MyPiEntity> sizeBoundReportableEntityStore;

    @Before
    public void setup() {
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        piEntity2 = new MyPiEntity(NODE_ID, 2);
        sizeBoundReportableEntityStore = new SizeBoundReportableEntityStore<MyPiEntity>(2);
    }

    @Test
    public void shouldAddIndividualEntities() throws Exception {
        // act
        sizeBoundReportableEntityStore.add(piEntity1);
        sizeBoundReportableEntityStore.add(piEntity2);

        // assert
        assertThat(sizeBoundReportableEntityStore.getAllEntities().size(), equalTo(2));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity1), is(true));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity2), is(true));
    }

    @Test
    public void shouldAddCollectionOfEntities() throws Exception {
        // act
        sizeBoundReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1, piEntity2 }));

        // assert
        assertThat(sizeBoundReportableEntityStore.getAllEntities().size(), equalTo(2));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity1), is(true));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity2), is(true));
    }

    @Test
    public void shouldOnlyStoreMostRecentEntities() throws Exception {
        // setup
        MyPiEntity piEntity3 = new MyPiEntity(NODE_ID, 3);
        sizeBoundReportableEntityStore.add(piEntity3);

        // act
        sizeBoundReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1, piEntity2 }));

        // assert
        assertThat(sizeBoundReportableEntityStore.getAllEntities().size(), equalTo(2));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity3), is(true));
        assertThat(sizeBoundReportableEntityStore.getAllEntities().contains(piEntity2), is(true));
    }

    @Test
    public void lookAtPublishIterator() throws Exception {
        // setup
        MyPiEntity piEntity3 = new MyPiEntity(NODE_ID, 3);
        sizeBoundReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1, piEntity2, piEntity3 }));

        // act
        Iterator<MyPiEntity> publishIterator = sizeBoundReportableEntityStore.getPublishIterator();

        // assert
        boolean hasPiEntity1 = false, hasPiEntity2 = false, hasPiEntity3 = false;
        while (publishIterator.hasNext()) {
            MyPiEntity piEntity = publishIterator.next();
            hasPiEntity1 |= piEntity.equals(piEntity1);
            hasPiEntity2 |= piEntity.equals(piEntity2);
            hasPiEntity3 |= piEntity.equals(piEntity3);
        }
        assertThat(hasPiEntity1, is(false));
        assertThat(hasPiEntity2, is(true));
        assertThat(hasPiEntity3, is(true));
    }

    @Test
    public void existsShouldReturnTrueForOlderEntity() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        Thread.sleep(10);
        piEntity2 = new MyPiEntity(NODE_ID, 1);
        sizeBoundReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity2 }));

        // act
        boolean result = sizeBoundReportableEntityStore.exists(piEntity1);

        // assert
        assertTrue(result);
    }

    @Test
    public void existsShouldReturnTrueForNewerEntity() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        Thread.sleep(10);
        piEntity2 = new MyPiEntity(NODE_ID, 1);
        sizeBoundReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1 }));

        // act
        boolean result = sizeBoundReportableEntityStore.exists(piEntity2);

        // assert
        assertTrue(result);
    }

    private class MyPiEntity extends ReportableEntity<MyPiEntity> {
        private int value = 0;
        private long creationTime;

        public MyPiEntity(String nodeId, int value) {
            super(nodeId);
            this.value = value;
            this.creationTime = System.currentTimeMillis();
        }

        @Override
        public boolean equals(Object other) {
            MyPiEntity otherEntity = (MyPiEntity) other;
            boolean result = this.getNodeId().equals(otherEntity.getNodeId());
            result &= this.value == otherEntity.value;
            return result;
        }

        @Override
        public int hashCode() {
            return getNodeId().hashCode() + value;
        }

        @Override
        public Object[] getKeysForMap() {
            return new String[] { getNodeId() };
        }

        @Override
        public int getKeysForMapCount() {
            return getKeysForMap().length;
        }

        @Override
        public int compareTo(MyPiEntity o) {
            return value - o.value;
        }

        @Override
        public String getType() {
            return this.getClass().getName();
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public long getCreationTime() {
            return creationTime;
        }

        @Override
        public String getUriScheme() {
            return this.getClass().getName();
        }
    }
}

package com.bt.pi.core.application.reporter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class NodeBasedReportableEntityStoreTest {
    private static final String NODE_ID = "nodeId";
    private MyPiEntity piEntity1;
    private MyPiEntity piEntity2;
    private NodeBasedReportableEntityStore<MyPiEntity> nodeBasedReportableEntityStore;

    @Before
    public void setup() {
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        piEntity2 = new MyPiEntity(NODE_ID, 2);
        nodeBasedReportableEntityStore = new NodeBasedReportableEntityStore<MyPiEntity>();
    }

    @Test
    public void shouldStoreNewEntityForNodeIfNewer() throws Exception {
        // setup
        nodeBasedReportableEntityStore.add(piEntity1);

        // act
        nodeBasedReportableEntityStore.add(piEntity2);

        // assert
        assertThat(nodeBasedReportableEntityStore.getByNodeId(NODE_ID), equalTo(piEntity2));
    }

    @Test
    public void shouldRetainNewEntityForNodeIfOlder() throws Exception {
        // setup
        nodeBasedReportableEntityStore.add(piEntity2);

        // act
        nodeBasedReportableEntityStore.add(piEntity1);

        // assert
        assertThat(nodeBasedReportableEntityStore.getByNodeId(NODE_ID), equalTo(piEntity2));
    }

    @Test
    public void shouldAddAllEntitiesKeepingOnlyNewerPerNode() throws Exception {
        // setup
        MyPiEntity piEntity3 = new MyPiEntity("new", 0);

        // act
        nodeBasedReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1, piEntity2, piEntity3 }));

        // assert
        assertThat(nodeBasedReportableEntityStore.getAllEntities().size(), equalTo(2));
        assertThat(nodeBasedReportableEntityStore.getByNodeId(NODE_ID), equalTo(piEntity2));
        assertThat(nodeBasedReportableEntityStore.getByNodeId("new"), equalTo(piEntity3));
    }

    @Test
    public void shouldAddAllEntitiesAndThenLookAtPublishIterator() throws Exception {
        // setup
        MyPiEntity piEntity3 = new MyPiEntity("new", 0);
        nodeBasedReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1, piEntity2, piEntity3 }));

        // act
        Iterator<MyPiEntity> publishIterator = nodeBasedReportableEntityStore.getPublishIterator();

        // assert
        boolean hasPiEntity2 = false, hasPiEntity3 = false;
        while (publishIterator.hasNext()) {
            MyPiEntity piEntity = publishIterator.next();
            hasPiEntity2 |= piEntity.equals(piEntity2);
            hasPiEntity3 |= piEntity.equals(piEntity3);
        }
        assertThat(hasPiEntity2, is(true));
        assertThat(hasPiEntity3, is(true));
    }

    @Test
    public void shouldDetectExistingEntities() throws Exception {
        Thread.sleep(10);
        MyPiEntity piEntity12 = new MyPiEntity(NODE_ID, 1);
        MyPiEntity piEntity22 = new MyPiEntity(NODE_ID, 2);
        nodeBasedReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity12, piEntity22 }));

        assertTrue("piEntity1 should exist", nodeBasedReportableEntityStore.exists(piEntity1));
        assertTrue("piEntity2 should exist", nodeBasedReportableEntityStore.exists(piEntity2));
    }

    @Test
    public void existsShouldReturnTrueForOlderEntity() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        Thread.sleep(10);
        piEntity2 = new MyPiEntity(NODE_ID, 1);
        nodeBasedReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity2 }));

        // act
        boolean result = nodeBasedReportableEntityStore.exists(piEntity1);

        // assert
        assertTrue(result);
    }

    @Test
    public void existsShouldReturnFalseForNewerEntity() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(NODE_ID, 1);
        Thread.sleep(30);
        piEntity2 = new MyPiEntity(NODE_ID, 1);
        nodeBasedReportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity1 }));

        // act
        boolean result = nodeBasedReportableEntityStore.exists(piEntity2);

        // assert
        assertFalse(result);
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
            return this.value == ((MyPiEntity) other).value && this.getNodeId().equals(((MyPiEntity) other).getNodeId());
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public Object[] getKeysForMap() {
            return null;
        }

        @Override
        public int getKeysForMapCount() {
            return 0;
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
            return this.creationTime;
        }

        @Override
        public String getUriScheme() {
            return this.getClass().getName();
        }
    }
}

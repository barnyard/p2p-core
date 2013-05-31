package com.bt.pi.core.application.reporter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class TimeBasedReportableEntityStoreTest {
    protected MyPiEntity piEntity1;
    protected MyPiEntity piEntity2;
    protected MyPiEntity piEntity3;
    protected MyPiEntity piEntity4;
    protected MyPiEntity piEntity5;
    protected ReportableEntityStore reportableEntityStore;

    @Before
    public void setup() {
        piEntity1 = new MyPiEntity(1);
        piEntity2 = new MyPiEntity(2);
        piEntity3 = new MyPiEntity(3);
        piEntity4 = new MyPiEntity(4);
        piEntity5 = new MyPiEntity(5);
        reportableEntityStore = new TimeBasedReportableEntityStore<MyPiEntity>("test" + new Random().nextInt(), 120, 1);
    }

    @Test
    public void shouldAddIndividuallyAndThroughCollections() throws Exception {
        // act
        reportableEntityStore.add(piEntity1);
        reportableEntityStore.add(piEntity2);
        reportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity3, piEntity4 }));
        reportableEntityStore.add(piEntity5);

        // assert
        Collection<MyPiEntity> allEntities = reportableEntityStore.getAllEntities();
        assertThat(allEntities.size(), equalTo(5));
        assertThat(allEntities.contains(piEntity1), is(true));
        assertThat(allEntities.contains(piEntity2), is(true));
        assertThat(allEntities.contains(piEntity3), is(true));
        assertThat(allEntities.contains(piEntity4), is(true));
        assertThat(allEntities.contains(piEntity5), is(true));
    }

    @Test
    public void shouldOnlyReturnNonExpiredItems() throws Exception {
        // setup
        reportableEntityStore.add(piEntity1);
        Thread.sleep(2500);
        MyPiEntity e2 = new MyPiEntity(2);
        reportableEntityStore.add(e2);

        MyPiEntity e3 = new MyPiEntity(3);
        MyPiEntity e4 = new MyPiEntity(4);
        reportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { e3, e4 }));

        MyPiEntity e5 = new MyPiEntity(5);
        reportableEntityStore.add(e5);

        // act
        Collection<MyPiEntity> allEntities = reportableEntityStore.getAllEntities();

        // assert
        assertThat(allEntities.size(), equalTo(4));
        assertThat(allEntities.contains(piEntity1), is(false));
        assertThat(allEntities.contains(e2), is(true));
        assertThat(allEntities.contains(e3), is(true));
        assertThat(allEntities.contains(e4), is(true));
        assertThat(allEntities.contains(e5), is(true));
    }

    @Test
    public void shouldTestPublishIterator() throws Exception {
        // setup
        reportableEntityStore.add(piEntity1);
        Thread.sleep(2500);

        MyPiEntity e2 = new MyPiEntity(2);
        MyPiEntity e3 = new MyPiEntity(3);
        reportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { e2, e3 }));

        // act
        Iterator<MyPiEntity> publishIterator = reportableEntityStore.getPublishIterator();

        // assert
        boolean hasPiEntity1 = false, hasPiEntity2 = false, hasPiEntity3 = false;
        while (publishIterator.hasNext()) {
            MyPiEntity piEntity = publishIterator.next();
            hasPiEntity1 |= piEntity.equals(piEntity1);
            hasPiEntity2 |= piEntity.equals(e2);
            hasPiEntity3 |= piEntity.equals(e3);
        }
        assertThat(hasPiEntity1, is(false));
        assertThat(hasPiEntity2, is(true));
        assertThat(hasPiEntity3, is(true));
    }

    @Test
    public void shouldDetectExistingEntities() throws Exception {
        Thread.sleep(10);
        MyPiEntity piEntity11 = new MyPiEntity(1);
        MyPiEntity piEntity22 = new MyPiEntity(2);
        MyPiEntity piEntity33 = new MyPiEntity(3);
        reportableEntityStore.addAll(Arrays.asList(new MyPiEntity[] { piEntity11, piEntity22, piEntity33 }));

        assertTrue("piEntity1 should exist", reportableEntityStore.exists(piEntity1));
        assertTrue("piEntity2 should exist", reportableEntityStore.exists(piEntity2));
        assertTrue("piEntity3 should exist", reportableEntityStore.exists(piEntity3));
    }

    @Test
    public void shouldReturnFalseForNonExistentEntities() {
        assertFalse("piEntity1 should not exist", reportableEntityStore.exists(piEntity1));
    }

    @Test
    public void shouldReturnTrueForOlderEntities() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(1);
        Thread.sleep(10);
        piEntity2 = new MyPiEntity(1);
        reportableEntityStore.add(piEntity2);

        // act
        boolean result = reportableEntityStore.exists(piEntity1);

        // assert
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseForNewerEntities() throws Exception {
        // setup
        piEntity1 = new MyPiEntity(1);
        Thread.sleep(10);
        piEntity2 = new MyPiEntity(1);
        reportableEntityStore.add(piEntity1);

        // act
        boolean result = reportableEntityStore.exists(piEntity2);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldUseDefaultConstructorForTimeBasedReportableEntity() {
        // act
        new MyPiEntity();
    }

    protected class MyPiEntity extends TimeBasedReportableEntity<MyPiEntity> {
        private int id;
        private long creationTime;

        public MyPiEntity(int id) {
            super("");
            this.id = id;
            this.creationTime = new Date().getTime();
        }

        public MyPiEntity() {
            super();
            this.creationTime = new Date().getTime();
        }

        public void setCreationTime(long creationTime) {
            this.creationTime = creationTime;
        }

        @Override
        public boolean equals(Object other) {
            return this.id == ((MyPiEntity) other).id;
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
            return 0;
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
        public Object getId() {
            return id;
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

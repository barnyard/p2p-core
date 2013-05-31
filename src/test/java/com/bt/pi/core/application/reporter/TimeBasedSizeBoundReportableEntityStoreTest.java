package com.bt.pi.core.application.reporter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class TimeBasedSizeBoundReportableEntityStoreTest extends TimeBasedReportableEntityStoreTest {
    private int size = 3;

    @Test
    public void shouldOnlyStoreUptoSizeElements() throws Exception {
        // setup
        reportableEntityStore = new TimeBasedSizeBoundReportableEntityStore<MyPiEntity>("test" + new Random().nextInt(), 120, 1, size);

        // act
        reportableEntityStore.addAll(Arrays.asList(piEntity1, piEntity2, piEntity3, piEntity4, piEntity5));

        // assert
        Collection<MyPiEntity> allEntities = reportableEntityStore.getAllEntities();
        assertThat(allEntities.size(), equalTo(size));
    }

    @Test
    public void shouldOnlyStoreNewestAccordingToCreationTime() throws Exception {
        // setup
        reportableEntityStore = new TimeBasedSizeBoundReportableEntityStore<MyPiEntity>("test" + new Random().nextInt(), 120, 1, size);

        long now = System.currentTimeMillis();
        piEntity1.setCreationTime(now - 100);
        piEntity2.setCreationTime(now - 500);
        piEntity3.setCreationTime(now - 200);
        piEntity4.setCreationTime(now - 400);
        piEntity5.setCreationTime(now - 300);

        // act
        reportableEntityStore.addAll(Arrays.asList(piEntity1, piEntity2, piEntity3, piEntity4, piEntity5));

        // assert
        Collection<MyPiEntity> allEntities = reportableEntityStore.getAllEntities();
        assertThat(allEntities.size(), equalTo(size));
        assertThat(allEntities.contains(piEntity1), is(true));
        assertThat(allEntities.contains(piEntity2), is(false));
        assertThat(allEntities.contains(piEntity3), is(true));
        assertThat(allEntities.contains(piEntity4), is(false));
        assertThat(allEntities.contains(piEntity5), is(true));
    }
}

package com.bt.pi.core.entity;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TaskProcessingQueueItemTest {
    private TaskProcessingQueueItem taskProcessingQueueItem;

    @Before
    public void setUp() throws Exception {
        this.taskProcessingQueueItem = new TaskProcessingQueueItem();
    }

    @Test
    public void testResetLastUpdatedMillis() throws InterruptedException {
        // assert
        assertLastResetUpdatedMillis(this.taskProcessingQueueItem.getLastUpdatedMillis(), 50);

        // act
        Thread.sleep(300);
        this.taskProcessingQueueItem.resetLastUpdatedMillis();

        // assert
        assertLastResetUpdatedMillis(this.taskProcessingQueueItem.getLastUpdatedMillis(), 50);
    }

    private void assertLastResetUpdatedMillis(long lastUpdatedMillis, int wobbleFactor) {
        assertTrue(Math.abs(lastUpdatedMillis - System.currentTimeMillis()) < wobbleFactor);
    }
}

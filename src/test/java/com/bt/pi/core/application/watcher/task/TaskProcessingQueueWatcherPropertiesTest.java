package com.bt.pi.core.application.watcher.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;

public class TaskProcessingQueueWatcherPropertiesTest {
    @TaskProcessingQueueWatcherProperties
    class MyQueueWatcher {
    }

    private TaskProcessingQueueWatcherProperties taskProcessingQueueWatcherProperties;

    @Before
    public void setup() {
        MyQueueWatcher queueWatcher = new MyQueueWatcher();
        taskProcessingQueueWatcherProperties = queueWatcher.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

    }

    @Test
    public void testDefaultStaleItemDuration() {
        // act
        int result = taskProcessingQueueWatcherProperties.staleQueueItemMillis();

        // assert
        assertThat(result, equalTo(30 * 60 * 1000));
    }

    @Test
    public void testDefaultInitialInterval() {
        // act
        long result = taskProcessingQueueWatcherProperties.initialQueueWatcherIntervalMillis();

        // assert
        assertThat(result, equalTo(3 * 60 * 1000L));
    }

    @Test
    public void testDefaultRepeatingInterval() {
        // act
        long result = taskProcessingQueueWatcherProperties.repeatingQueueWatcherIntervalMillis();

        // assert
        assertThat(result, equalTo(3 * 60 * 1000L));
    }
}

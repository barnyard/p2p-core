package com.bt.pi.core.application.watcher.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.watcher.WatcherApplication;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.KoalaIdFactory;

@RunWith(MockitoJUnitRunner.class)
public class TaskProcessingQueueWatcherInitiatorBaseTest {
    private String watcherName = "watcher";
    private int staleQueueItemMillis = 1;
    private long initialQueueWatcherIntervalMillis = 2;
    private long repeatingQueueWatcherIntervalMillis = 3;

    @Mock
    private PiLocation piLocation;
    @Mock
    private WatcherService watcherService;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private WatcherApplication watcherApplication;

    @InjectMocks
    private TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiatorBase = new TaskProcessingQueueWatcherInitiatorBase(watcherName, piLocation) {
    };

    @Before
    public void setup() {
        taskProcessingQueueWatcherInitiatorBase.setStaleQueueItemMillis(staleQueueItemMillis);
        taskProcessingQueueWatcherInitiatorBase.setInitialQueueWatcherIntervalMillis(initialQueueWatcherIntervalMillis);
        taskProcessingQueueWatcherInitiatorBase.setRepeatingQueueWatcherIntervalMillis(repeatingQueueWatcherIntervalMillis);
    }

    @Test
    public void shouldCreateWatcher() throws Exception {
        // act
        taskProcessingQueueWatcherInitiatorBase.createTaskProcessingQueueWatcher("nodeId");

        // assert
        verify(watcherService).replaceTask(eq(watcherName), isA(TaskProcessingQueueWatcher.class), eq(initialQueueWatcherIntervalMillis), eq(repeatingQueueWatcherIntervalMillis));
    }

    @Test
    public void testGetters() throws Exception {
        // act & assert
        assertThat(taskProcessingQueueWatcherInitiatorBase.getDhtClientFactory(), equalTo(dhtClientFactory));
        assertThat(taskProcessingQueueWatcherInitiatorBase.getKoalaIdFactory(), equalTo(koalaIdFactory));
        assertThat(taskProcessingQueueWatcherInitiatorBase.getStaleQueueItemMillis(), equalTo(staleQueueItemMillis));
    }

    @Test
    public void shouldRemoveWatcher() {
        // act
        this.taskProcessingQueueWatcherInitiatorBase.removeTaskProcessingQueueWatcher();

        // assert
        verify(watcherService).removeTask(watcherName);
    }
}

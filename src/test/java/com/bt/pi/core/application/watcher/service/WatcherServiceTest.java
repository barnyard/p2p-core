package com.bt.pi.core.application.watcher.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;


public class WatcherServiceTest {
    private WatcherService watcherService;
    private ScheduledExecutorService scheduledExecutorService;
    private Runnable runnable;
    private Semaphore runnableCalledSemaphore;

    @Before
    public void before() {
        runnableCalledSemaphore = new Semaphore(0);
        runnable = new Runnable() {
            @Override
            public void run() {
                runnableCalledSemaphore.release();
            }
        };

        scheduledExecutorService = Executors.newScheduledThreadPool(4);

        watcherService = new WatcherService();
        watcherService.setScheduledExecutorService(scheduledExecutorService);

        watcherService.start();
    }

    private int getTasksInQueue() {
        synchronized (watcherService) {
            int num = 0;
            for (Entry<Long, List<WatcherServiceTask>> e : watcherService.getTaskQueue().entrySet()) {
                num += e.getValue().size();
            }
            return num;
        }
    }

    @Test
    public void shouldKickOffProcessingOnStart() throws InterruptedException {
        // setup
        final CountDownLatch processingLatch = new CountDownLatch(1);
        watcherService = new WatcherService() {
            @Override
            protected void processQueuedTasks() throws InterruptedException {
                processingLatch.countDown();
            }
        };

        // act
        watcherService.start();

        // assert
        assertTrue(processingLatch.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void shouldExecuteQueuedTask() throws InterruptedException {
        // act
        watcherService.addTask("mytask", runnable, 0, 60 * 1000);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(3, TimeUnit.SECONDS));
    }

    @Test
    public void shouldExecuteMultipleTasksQueuedTogether() throws InterruptedException {
        // act
        watcherService.addTask("mytask", runnable, 0, 60 * 1000);
        watcherService.addTask("thytask", runnable, 0, 60 * 1000);
        watcherService.addTask("whytask", runnable, 0, 60 * 1000);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(3, 3, TimeUnit.SECONDS));
    }

    @Test
    public void shouldRepeatedlyExecuteQueuedTask() throws InterruptedException {
        // act
        watcherService.addTask("mytask", runnable, 0, 500);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(3, 3, TimeUnit.SECONDS));
    }

    @Test(expected = TaskAlreadyExistsException.class)
    public void shouldNotAllowDuplicateTaskNames() throws InterruptedException {
        // act
        watcherService.addTask("mytask", runnable, 0, 500);
        watcherService.addTask("mytask", runnable, 0, 500);
    }

    @Test
    public void shouldNotReExecuteTaskUntilDone() throws InterruptedException {
        // setup
        runnable = new Runnable() {
            @Override
            public void run() {
                runnableCalledSemaphore.release();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        // act
        watcherService.addTask("mytask", runnable, 0, 200);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertFalse(runnableCalledSemaphore.tryAcquire(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldAbsorbExceptionInTask() throws InterruptedException {
        final AtomicInteger numRuns = new AtomicInteger(0);
        runnable = new Runnable() {
            @Override
            public void run() {
                if (numRuns.incrementAndGet() <= 1)
                    throw new RuntimeException("boo");
                else
                    runnableCalledSemaphore.release();
            }
        };

        // act
        watcherService.addTask("mytask", runnable, 0, 500);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(3, TimeUnit.SECONDS));
    }

    @Test
    public void shouldBeAbleToRemoveTask() throws InterruptedException {
        // setup
        watcherService.addTask("mytask", runnable, 0, 50000);
        assertTrue(runnableCalledSemaphore.tryAcquire(3, TimeUnit.SECONDS));

        // act
        watcherService.removeTask("mytask");

        // assert
        assertFalse(runnableCalledSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertEquals(0, watcherService.getTaskQueue().get(watcherService.getTaskQueue().firstKey()).size());
    }

    @Test
    public void shouldRemoveTaskButLeaveOthers() throws InterruptedException {
        // setup
        watcherService.addTask("mytask", runnable, 5000, 5000);
        watcherService.addTask("thytask", runnable, 5000, 5000);
        watcherService.addTask("whytask", runnable, 5000, 5000);

        // act
        watcherService.removeTask("mytask");

        // assert
        assertEquals(2, getTasksInQueue());
    }

    @Test
    public void shouldBeAbleToReplaceTask() throws InterruptedException {
        // setup
        watcherService.addTask("mytask1", runnable, 0, 50000);
        watcherService.addTask("mytask2", runnable, 0, 50000);
        assertTrue(runnableCalledSemaphore.tryAcquire(2, 3, TimeUnit.SECONDS));

        // act
        watcherService.replaceTask("mytask1", runnable, 0, 30000);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertEquals(2, getTasksInQueue());
    }

    @Test
    public void watcherWantsToDoMoreButShouldnt() throws Exception {
        final AtomicInteger slowPokeCounter = new AtomicInteger(0);
        watcherService.addTask("Slow poke task", new Runnable() {
            @Override
            public void run() {
                slowPokeCounter.incrementAndGet();
                try {
                    Thread.sleep(20 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, 1L, 500);

        Thread.sleep(10 * 1000);

        System.err.println("count: " + slowPokeCounter.get());
        assertEquals(1, slowPokeCounter.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldExecuteQueuedTasksByAddingRandomness() throws InterruptedException {
        // setup
        final SortedMap<Long, List<WatcherServiceTask>> taskQueue = spy(new TreeMap<Long, List<WatcherServiceTask>>());
        watcherService = new WatcherService() {
            @Override
            protected SortedMap<Long, List<WatcherServiceTask>> getTaskQueue() {
                return taskQueue;
            }
        };
        watcherService.setRandomInterval(.05f);
        watcherService.setScheduledExecutorService(scheduledExecutorService);
        watcherService.start();

        final AtomicInteger counter = new AtomicInteger();
        final AtomicLong time = new AtomicLong(System.currentTimeMillis());

        // act
        watcherService.addTask("mytask", runnable, 1000, 1000);

        // assert
        assertTrue(runnableCalledSemaphore.tryAcquire(3, 5, TimeUnit.SECONDS));
        verify(taskQueue, atLeast(3)).put(argThat(new ArgumentMatcher<Long>() {
            @Override
            public boolean matches(Object argument) {
                if (counter.get() >= 3)
                    return true;

                Long when = (Long) argument;
                System.out.println("when: " + when);
                System.out.println("time: " + time.get());
                if (when >= (time.get() + 500) && when <= (time.get() + 1500)) {
                    counter.incrementAndGet();
                    time.getAndSet(when);
                    System.out.println("return true");
                    return true;
                }

                time.getAndSet(when);
                System.out.println("return false");
                return false;

            }
        }), isA(List.class));
    }
}

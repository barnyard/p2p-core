package com.bt.pi.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.TaskExecutor;

@RunWith(MockitoJUnitRunner.class)
public class SerialExecutorTest {
    @InjectMocks
    private SerialExecutor serialExecutor = new SerialExecutor();
    @Mock
    private TaskExecutor executor;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        serialExecutor.start();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                new Thread(r).start();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldOnlyExecuteRunnablesWhichImplementSerialExecutorRunnable() throws Exception {
        // setup
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };

        // act
        serialExecutor.execute(r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void urlOfSerialExecutorRunnableShouldNotBeNull() throws Exception {
        // setup
        Runnable r = new SerialExecutorRunnable(null, null) {
            @Override
            public void run() {
            }
        };

        // act
        serialExecutor.execute(r);
    }

    @Test
    public void shouldAddItemToQueueAndRunIt() throws InterruptedException {
        // setup
        final CountDownLatch latch1 = new CountDownLatch(1);
        Runnable r = new SerialExecutorRunnable("r", "1") {
            @Override
            public void run() {
                latch1.countDown();
            }
        };

        // act
        serialExecutor.execute(r);

        // assert
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldRunTasksOneAtATime() throws InterruptedException {
        // setup
        final CountDownLatch latch1 = new CountDownLatch(1);
        Runnable r1 = new SerialExecutorRunnable("r", "1") {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                latch1.countDown();
            }
        };

        final CountDownLatch latch2 = new CountDownLatch(1);
        Runnable r2 = new SerialExecutorRunnable("r", "2") {
            @Override
            public void run() {
                try {
                    assertTrue(latch1.await(0, TimeUnit.MILLISECONDS));
                    latch2.countDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        // act
        serialExecutor.execute(r1);
        serialExecutor.execute(r2);

        // assert
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldContinueEvenIfOneTaskThrowsException() throws InterruptedException {
        // setup
        final CountDownLatch latch1 = new CountDownLatch(1);
        Runnable r1 = new SerialExecutorRunnable("r", "1") {
            @Override
            public void run() {
                latch1.countDown();
                throw new RuntimeException("shit happens");
            }
        };

        final CountDownLatch latch2 = new CountDownLatch(1);
        Runnable r2 = new SerialExecutorRunnable("r", "2") {
            @Override
            public void run() {
                latch2.countDown();
            }
        };

        // act
        serialExecutor.execute(r1);
        serialExecutor.execute(r2);

        // assert
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void shouldConfirmExistenceOfQueuedTask() throws InterruptedException {
        // setup
        SerialExecutorRunnable r1 = new SerialExecutorRunnable("r", "1") {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        SerialExecutorRunnable r2 = new SerialExecutorRunnable("r", "2") {
            @Override
            public void run() {
            }
        };

        // act
        serialExecutor.execute(r1);
        serialExecutor.execute(r2);

        // assert
        assertTrue(serialExecutor.isQueuedOrRunning("r", "1"));
        assertTrue(serialExecutor.isQueuedOrRunning("r", "2"));
        assertFalse(serialExecutor.isQueuedOrRunning("r", "3"));
    }
}

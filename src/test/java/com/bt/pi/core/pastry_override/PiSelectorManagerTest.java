package com.bt.pi.core.pastry_override;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rice.environment.time.TimeSource;
import rice.selector.LoopObserver;
import rice.selector.TimerTask;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PiSelectorManagerTest {

    @Resource
    private PiSelectorManager selectorManager;
    private Runnable simpleRunnable;
    private Runnable simpleRunnable2;
    private LoopObserver loopObserver;
    private LoopObserver loopObserver2;
    private TimerTask timerTask;
    private Selector selector;

    @Before
    public void testBefore() {
        selector = mock(Selector.class);

        selectorManager.setSelector(selector);
        selectorManager.setMaxExecuteDueTaskTimeMillis(99999);

        simpleRunnable = mock(Runnable.class);
        simpleRunnable2 = mock(Runnable.class);
        loopObserver = mock(LoopObserver.class);
        loopObserver2 = mock(LoopObserver.class);
        timerTask = mock(TimerTask.class);
        selectorManager.getPiLoopObservers().clear();

    }

    public void addMockInvocations() {
        selectorManager.invoke(simpleRunnable);
        selectorManager.invoke(simpleRunnable2);
    }

    @Test
    public void addLoopObserver() {

        // act
        selectorManager.addLoopObserver(loopObserver);

        // assert
        assertEquals(1, selectorManager.getPiLoopObservers().size());
        assertEquals(loopObserver, selectorManager.getPiLoopObservers().get(0));
    }

    @Test
    public void removeLoopObserver() {
        // setup

        System.err.println("PiLoopObservers Size: " + selectorManager.getPiLoopObservers().size());
        selectorManager.getPiLoopObservers().add(loopObserver);
        selectorManager.getPiLoopObservers().add(loopObserver2);
        System.err.println("PiLoopObservers Size: " + selectorManager.getPiLoopObservers().size());
        // act
        selectorManager.removeLoopObserver(loopObserver2);
        System.err.println("PiLoopObservers Size: " + selectorManager.getPiLoopObservers().size());
        assertEquals(1, selectorManager.getPiLoopObservers().size());
        assertEquals(loopObserver, selectorManager.getPiLoopObservers().get(0));
    }

    @Test
    public void testNotifyLoopListeners() {
        // setup
        selectorManager.getPiLoopObservers().add(loopObserver);
        when(loopObserver.delayInterest()).thenReturn(99999999);

        // act
        selectorManager.notifyLoopListeners();

        // verify
        verify(loopObserver).loopTime(anyInt());
    }

    @Test
    public void testInvocationsSize() {
        // setup
        addMockInvocations();

        // act
        int result = selectorManager.getNumInvocations();

        // assert
        assertEquals(2, result);
    }

    @Test
    public void testDoInvocations() {
        // setup
        addMockInvocations();

        // act
        selectorManager.doInvocations();

        // verify
        verify(simpleRunnable).run();
        verify(simpleRunnable2).run();
    }

    @Test
    public void testDoInvocationsIsTimeBound() {
        // setup
        selectorManager.setMaxInvocationTimeMillis(50);
        selectorManager.invoke(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        addMockInvocations();

        // act
        selectorManager.doInvocations();

        // assert
        verify(simpleRunnable, never()).run();
        verify(simpleRunnable2, never()).run();
        assertEquals(2, selectorManager.getNumInvocations());

    }

    @Test
    public void testDoInvocationsRemovesModifiedKeys() throws Exception {
        // setup
        SelectionKey key = mock(SelectionKey.class);
        selectorManager.modifyKey(key);
        selectorManager.setMaxInvocationTimeMillis(80);

        // act
        selectorManager.doInvocations();

        // assert

        assertNull(selectorManager.getModifyKey());
        assertEquals(0, selectorManager.getPiModifiedKeys().size());
    }

    @Test
    public void testGetInvocation() {
        // setup
        addMockInvocations();

        // act
        Runnable result = selectorManager.getInvocation();

        assertEquals(simpleRunnable, result);

    }

    @Test
    public void testAddTimerTask() throws InterruptedException {

        // act
        addTimerTask();

        // verify
        verify(timerTask).setSelectorManager(selectorManager);
        verify(selector).wakeup();
        assertEquals(timerTask, selectorManager.getPiTimerQueue().take());

    }

    private void addTimerTask() {
        try {
            selectorManager.addTask(timerTask);
        } catch (IllegalMonitorStateException imse) {
            // we catch this because selector is a thread...
        }
    }

    @Test
    public void testRemoveTimerTask() throws Exception {
        // setup
        addTimerTask();

        // act
        selectorManager.removeTask(timerTask);

        // assert
        assertEquals(0, selectorManager.getPiTimerQueue().size());
    }

    @Test
    public void testGetNextTaskExecutionTime() throws Exception {
        // setup
        addTimerTask();
        long expected = 99L;
        when(timerTask.scheduledExecutionTime()).thenReturn(expected);

        // act
        long result = selectorManager.getNextTaskExecutionTime();

        // assert
        assertEquals(expected, result);
    }

    @Test
    public void testExecuteDueTasks() {
        // setup
        when(timerTask.scheduledExecutionTime()).thenReturn(10000L);
        when(timerTask.execute((TimeSource) anyObject())).thenReturn(false);
        addTimerTask();

        // act
        selectorManager.executeDueTasks();

        // verify
        assertEquals(null, selectorManager.getPiTimerQueue().poll());
    }

    @Test
    public void testExecuteDueTasksOnlyExecutesDoTasks() {
        // setup
        when(timerTask.scheduledExecutionTime()).thenReturn(System.currentTimeMillis() + 99999999L);
        when(timerTask.execute((TimeSource) anyObject())).thenReturn(false);
        addTimerTask();

        // act
        selectorManager.executeDueTasks();

        // verify
        assertEquals(timerTask, selectorManager.getPiTimerQueue().poll());
    }

    @Test
    public void testExecuteDueTasksReaddsFixedRateTasks() {
        // setup
        when(timerTask.scheduledExecutionTime()).thenReturn(10000L);
        when(timerTask.execute((TimeSource) anyObject())).thenReturn(true);
        addTimerTask();

        // act
        selectorManager.executeDueTasks();

        // verify
        assertEquals(timerTask, selectorManager.getPiTimerQueue().poll());
    }

    @Test
    public void testExecuteDueTasksWontOverrunExecutionWindow() {
        // setup
        selectorManager.setMaxExecuteDueTaskTimeMillis(-100);
        when(timerTask.scheduledExecutionTime()).thenReturn(10000L);
        addTimerTask();

        // act
        selectorManager.executeDueTasks();

        // verify
        assertEquals(timerTask, selectorManager.getPiTimerQueue().poll());
        verify(timerTask, never()).execute(isA(TimeSource.class));
    }

    @Test
    public void testSelect() throws Exception {

        // act
        selectorManager.select();

        // verify
        verify(selector).select(eq((long) selectorManager.getSelectorSleepTimeMillis()));
    }

    @Test
    public void testModifyKey() throws Exception {
        SelectionKey key = mock(SelectionKey.class);

        // act
        selectorManager.modifyKey(key);

        assertEquals(1, selectorManager.getPiModifiedKeys().size());
        assertEquals(key, selectorManager.getPiModifiedKeys().poll());
    }

    @Test
    public void testLockOnSelectorInvokesWakup() throws Exception {

        synchronized (selectorManager) {
            selectorManager.wakeup();
        }

    }

    @Test
    public void testGetModifyKey() {
        // setup
        SelectionKey key = mock(SelectionKey.class);

        // act
        try {
            selectorManager.modifyKey(key);
        } catch (IllegalMonitorStateException imse) {
            // void
        }

        // act
        SelectionKey result = selectorManager.getModifyKey();

        // assert
        assertEquals(key, result);
    }
}
package com.bt.pi.core.continuation.scattergather;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunner;

public class ScatterGatherContinuationRunnerTest {
    private Collection<ScatterGatherContinuationRunnable> runnables;
    private ScheduledExecutorService scheduledExecutorService;
    private ScatterGatherContinuationRunner scatterGatherContinuationRunner;

    @Before
    public void setup() {
        scheduledExecutorService = mock(ScheduledExecutorService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(scheduledExecutorService).execute(isA(Runnable.class));

        scatterGatherContinuationRunner = new ScatterGatherContinuationRunner();
        scatterGatherContinuationRunner.setScheduledExecutorService(scheduledExecutorService);
    }

    @Before
    public void setupCallables() {
        runnables = new ArrayList<ScatterGatherContinuationRunnable>();

        for (int i = 0; i < 5; i++) {
            ScatterGatherContinuationRunnable runnable = mock(ScatterGatherContinuationRunnable.class);
            runnables.add(runnable);
        }
    }

    @Test
    public void executeInvokesSubmitForEachCallable() throws Exception {
        // act
        scatterGatherContinuationRunner.execute(runnables, 0, TimeUnit.NANOSECONDS);

        // assert
        for (Runnable runnable : runnables)
            verify(runnable).run();
    }
}

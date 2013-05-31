package com.bt.pi.core.node;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class PiApplicationFutureTaskTest {
    private static final String APPLICATION_NAME = "app1";

    @Test
    public void shouldSetApplicationNameWhenCreatingAFutureTask() {
        // setup
        Runnable runnable = mock(Runnable.class);

        // act
        PiApplicationFutureTask futureTask = new PiApplicationFutureTask(runnable, APPLICATION_NAME);

        // assert
        assertEquals(APPLICATION_NAME, futureTask.getApplicationName());
    }
}

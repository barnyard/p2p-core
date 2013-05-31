package com.bt.pi.core.continuation.scattergather;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.activation.GlobalScopedApplicationRecord;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.entity.PiEntity;

public class PiScatterGatherContinuationTest {
    private PiContinuation<PiEntity> continuation;
    private PiScatterGatherContinuation<PiEntity> piScatterGatherContinuation;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        continuation = mock(PiContinuation.class);

        piScatterGatherContinuation = new PiScatterGatherContinuation(continuation);
        piScatterGatherContinuation.setLatch(mock(CountDownLatch.class));
    }

    @Test
    public void testReceiveResult() throws Exception {
        // setup
        PiEntity existingEntity = new GlobalScopedApplicationRecord();

        // act
        piScatterGatherContinuation.receiveResult(existingEntity);

        // assert
        verify(continuation).receiveResult(existingEntity);
    }
}

package com.bt.pi.core.continuation.scattergather;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.entity.PiEntity;

public class UpdateResolvingPiScatterGatherContinuationTest {
    @SuppressWarnings("unchecked")
    private UpdateResolvingPiContinuation continuation;

    @SuppressWarnings("unchecked")
    private UpdateResolvingPiScatterGatherContinuation updateResolvingPiScatterGatherContinuation;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        continuation = mock(UpdateResolvingPiContinuation.class);

        updateResolvingPiScatterGatherContinuation = new UpdateResolvingPiScatterGatherContinuation(continuation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdate() throws Exception {
        // setup
        PiEntity existingEntity = mock(PiEntity.class);
        PiEntity requestedEntity = mock(PiEntity.class);

        // act
        updateResolvingPiScatterGatherContinuation.update(existingEntity, requestedEntity);

        // assert
        verify(continuation).update(existingEntity, requestedEntity);
    }
}

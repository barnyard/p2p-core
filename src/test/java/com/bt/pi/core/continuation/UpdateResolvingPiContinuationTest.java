package com.bt.pi.core.continuation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.core.entity.PiEntity;

public class UpdateResolvingPiContinuationTest {
    @Test
    public void dummyForAbstractClassUsedInApps() throws Exception {
        assertNotNull(new UpdateResolvingPiContinuation<PiEntity>() {
            @Override
            public void handleResult(PiEntity result) {
            }

            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return null;
            }
        });
    }
}

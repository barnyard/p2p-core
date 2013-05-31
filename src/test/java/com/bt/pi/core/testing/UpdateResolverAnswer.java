package com.bt.pi.core.testing;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.entity.PiEntity;

public class UpdateResolverAnswer implements Answer<PiEntity> {
    private PiEntity existing;
    private PiEntity result;

    public UpdateResolverAnswer(PiEntity anExisting) {
        existing = anExisting;
        result = null;
    }

    @SuppressWarnings("unchecked")
    public PiEntity answer(InvocationOnMock invocation) throws Throwable {
        boolean requestedParamAbsent = invocation.getArguments()[1] instanceof UpdateResolver;
        PiEntity requested;
        UpdateResolver<PiEntity> updateResolver;
        if (requestedParamAbsent) {
            requested = null;
            updateResolver = (UpdateResolver<PiEntity>) invocation.getArguments()[1];
        } else {
            requested = (PiEntity) invocation.getArguments()[1];
            updateResolver = (UpdateResolver<PiEntity>) invocation.getArguments()[2];
        }
        result = updateResolver.update(existing, requested);
        return result;
    }

    public PiEntity getResult() {
        return result;
    }
}

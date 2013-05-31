package com.bt.pi.core.testing;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.entity.PiEntity;

public class UpdateResolvingContinuationAnswer implements Answer<Object>{
	private PiEntity existing;
	private PiEntity result;
	private boolean forceUpdateResult;
	private PiEntity forcedUpdateResult;
	
	public UpdateResolvingContinuationAnswer(PiEntity anExisting) {
		existing = anExisting;
		result = null;
		forceUpdateResult = false;
		forcedUpdateResult = null;
	}
	
	public void forceUpdateResult(PiEntity aForcedUpdateResult) {
		this.forcedUpdateResult = aForcedUpdateResult;
		forceUpdateResult = true;
	}
	
	@SuppressWarnings("unchecked")
    public Object answer(InvocationOnMock invocation) throws Throwable {
		boolean requestedParamAbsent = invocation.getArguments()[1] instanceof Continuation;
		PiEntity requested;
		UpdateResolvingContinuation<PiEntity, Exception> updateResolvingContinuation;
		if (requestedParamAbsent) {
			requested = null;
			updateResolvingContinuation = (UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[1];
		} else {
			requested = (PiEntity) invocation.getArguments()[1];
			updateResolvingContinuation = (UpdateResolvingContinuation<PiEntity, Exception>) invocation.getArguments()[2];
		}
        result = updateResolvingContinuation.update(existing, requested);
        try {
        	if (forceUpdateResult)
        		updateResolvingContinuation.receiveResult(forcedUpdateResult);
        	else
        		updateResolvingContinuation.receiveResult(result);
        } catch (Exception e) {
        	updateResolvingContinuation.receiveException(e);
        }
        return null;
	}
	
	public PiEntity getResult() {
		return this.result;
	}
}

package com.bt.pi.core.testing;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;

public class ThrowingContinuationAnswer implements Answer<Object>{
	private Exception exception;
	
	public ThrowingContinuationAnswer(Exception e) {
		exception = e;
	}
	
	@SuppressWarnings("unchecked")
	public Object answer(InvocationOnMock invocation) throws Throwable {
		boolean requestedParamAbsent = invocation.getArguments()[1] instanceof Continuation;
		Continuation updateResolvingContinuation;
		if (requestedParamAbsent) {
			updateResolvingContinuation = (Continuation) invocation.getArguments()[1];
		} else {
			updateResolvingContinuation = (Continuation) invocation.getArguments()[2];
		}
        updateResolvingContinuation.receiveException(exception);
        return null;
	}
}

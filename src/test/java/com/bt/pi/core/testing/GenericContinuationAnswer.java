package com.bt.pi.core.testing;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;

public class GenericContinuationAnswer<T> implements Answer<Object> {
    private T result;
    private Integer continuationArgumentIndex;

    public GenericContinuationAnswer(T aResult) {
        this(aResult, null);
    }

    public GenericContinuationAnswer(T aResult, Integer aContinuationArgumentIndex) {
        result = aResult;
        continuationArgumentIndex = aContinuationArgumentIndex;
    }

    protected void setResult(T aResult) {
        this.result = aResult;
    }

    @SuppressWarnings("unchecked")
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Continuation<T, Exception> continuation;
        if (continuationArgumentIndex != null)
            continuation = (Continuation<T, Exception>) invocation.getArguments()[continuationArgumentIndex];
        else
            continuation = (Continuation<T, Exception>) findContinuationArgument(invocation);
        try {
            continuation.receiveResult(result);
        } catch (Exception e) {
            continuation.receiveException(e);
        }
        return null;
    }

    protected Object findContinuationArgument(InvocationOnMock invocationOnMock) {
        for (int i = 0; i < invocationOnMock.getArguments().length; i++)
            if (invocationOnMock.getArguments()[i] instanceof Continuation<?, ?>)
                return invocationOnMock.getArguments()[i];
        throw new RuntimeException("No continuation arguments when attempting to set up a generic continuation answer!");
    }
}

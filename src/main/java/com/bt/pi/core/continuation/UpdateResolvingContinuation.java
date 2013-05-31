package com.bt.pi.core.continuation;

import rice.Continuation;

public interface UpdateResolvingContinuation<T, E extends Exception> extends UpdateResolver<T>, Continuation<T, E> {
}

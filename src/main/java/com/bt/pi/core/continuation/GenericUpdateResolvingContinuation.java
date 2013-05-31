package com.bt.pi.core.continuation;


public abstract class GenericUpdateResolvingContinuation<T> extends GenericContinuation<T> implements UpdateResolvingContinuation<T, Exception> {
    public GenericUpdateResolvingContinuation() {
    }
}

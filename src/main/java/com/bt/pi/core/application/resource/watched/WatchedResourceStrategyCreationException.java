package com.bt.pi.core.application.resource.watched;

public class WatchedResourceStrategyCreationException extends RuntimeException {
    private static final long serialVersionUID = -907027739222425302L;

    public WatchedResourceStrategyCreationException(String message) {
        super(message);
    }

    public WatchedResourceStrategyCreationException(String message, Throwable t) {
        super(message, t);
    }
}

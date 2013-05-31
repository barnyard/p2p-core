package com.bt.pi.core.application.watcher.service;

public class TaskAlreadyExistsException extends RuntimeException {
    private static final long serialVersionUID = 7718105370763256164L;

    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}

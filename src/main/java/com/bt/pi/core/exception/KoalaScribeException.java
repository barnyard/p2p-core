package com.bt.pi.core.exception;

public class KoalaScribeException extends KoalaException {
    private static final long serialVersionUID = 1L;

    public KoalaScribeException(Exception e) {
        super(e);
    }

    public KoalaScribeException(String message) {
        super(message);
    }

    public KoalaScribeException(String message, Throwable t) {
        super(message, t);
    }
}

package com.bt.pi.core.conf;

public class IllegalAnnotationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalAnnotationException() {
    }

    public IllegalAnnotationException(Throwable t) {
        super(t);
    }

    public IllegalAnnotationException(String message) {
        super(message);
    }
}

package com.bt.pi.core.exception;

public class PiConfigurationException extends KoalaException {
    private static final long serialVersionUID = 2878359761302043846L;

    public PiConfigurationException(Exception e) {
        super(e);
    }

    public PiConfigurationException(String message) {
        super(message);
    }

}

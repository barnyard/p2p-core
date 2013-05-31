package com.bt.pi.core.exception;

import rice.p2p.past.PastException;

public class PiInsufficientResultsException extends PastException {

    private static final long serialVersionUID = 1L;

    public PiInsufficientResultsException(String msg) {
        super(msg);
    }
}

//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.exception;

public class KoalaException extends RuntimeException {

	private static final long serialVersionUID = -807594032123167938L;

	public KoalaException(Exception e) {
		super(e);
	}

	public KoalaException(String message) {
		super(message);
	}

	public KoalaException(String message, Throwable t) {
		super(message, t);
	}
}

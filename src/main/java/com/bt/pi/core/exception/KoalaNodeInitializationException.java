//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.exception;

public class KoalaNodeInitializationException extends KoalaException {
	private static final long serialVersionUID = 8279749788982965351L;

	public KoalaNodeInitializationException(String message) {
		super(message);
	}

	public KoalaNodeInitializationException(String message, Throwable t) {
		super(message, t);
	}
}

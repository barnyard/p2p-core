//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.exception;

public class KoalaScenarioException extends KoalaException {

	private static final long serialVersionUID = 1L;

	public KoalaScenarioException(String message) {
		super(message);
	}

	public KoalaScenarioException(String message, Throwable t) {
		super(message, t);
	}
}

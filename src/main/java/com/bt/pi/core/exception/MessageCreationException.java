//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.exception;

public class MessageCreationException extends KoalaException {
	private static final long serialVersionUID = 1519526634095892131L;

	public MessageCreationException(String message) {
		super(message);
	}

	public MessageCreationException(String message, Throwable t) {
		super(message, t);
	}
}

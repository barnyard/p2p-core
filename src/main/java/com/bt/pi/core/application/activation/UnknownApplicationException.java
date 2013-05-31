package com.bt.pi.core.application.activation;

public class UnknownApplicationException extends RuntimeException {
	private static final long serialVersionUID = 3808809765666020498L;

	public UnknownApplicationException(String message) {
		super(message);
	}
}

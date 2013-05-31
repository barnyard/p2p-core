package com.bt.pi.core.application.activation;

public class ApplicationAlreadyExistsException extends RuntimeException {
	private static final long serialVersionUID = 3808809765666020498L;

	public ApplicationAlreadyExistsException(String message) {
		super(message);
	}
}

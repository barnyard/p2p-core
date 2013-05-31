package com.bt.pi.core.exception;

import rice.p2p.past.PastException;

public class KoalaContentVersionMismatchException extends PastException {

	private static final long serialVersionUID = 1L;

	public KoalaContentVersionMismatchException(String msg) {
		super(msg);
	}
}

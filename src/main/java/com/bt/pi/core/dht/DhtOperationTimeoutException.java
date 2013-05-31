//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.dht;


public class DhtOperationTimeoutException extends RuntimeException {
	private static final long serialVersionUID = 3315561367144863422L;

	public DhtOperationTimeoutException(String message) {
		super(message);
	}
}

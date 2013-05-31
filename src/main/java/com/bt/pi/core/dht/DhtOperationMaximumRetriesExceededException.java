//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.dht;


public class DhtOperationMaximumRetriesExceededException extends RuntimeException {
	private static final long serialVersionUID = -3098621542919780127L;

	public DhtOperationMaximumRetriesExceededException(String message) {
		super(message);
	}
}

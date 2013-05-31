//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import rice.Continuation;

import com.bt.pi.core.exception.KoalaMessageTimeoutException;
import com.bt.pi.core.message.KoalaMessage;

public class ContinuationTimeoutRunner implements Runnable {
	private RequestState<Continuation<KoalaMessage, Exception>> requestState;
	private RequestWrapperBase<Continuation<KoalaMessage, Exception>, KoalaMessage> requestWrapper;
	private String correlationUUID;

	public ContinuationTimeoutRunner(String messageCorrelationUUID,
			RequestState<Continuation<KoalaMessage, Exception>> continuationRequestState,
			RequestWrapperBase<Continuation<KoalaMessage, Exception>, KoalaMessage> messageRequestWrapper) {
		requestState = continuationRequestState;
		requestWrapper = messageRequestWrapper;
		correlationUUID = messageCorrelationUUID;
	}

	@Override
	public void run() {
		boolean gotResponse = requestWrapper.awaitResponse(requestState);
		if (!gotResponse) {
			requestState.getResponse().receiveException(
					new KoalaMessageTimeoutException("Message with correlation UUID: " + correlationUUID
							+ " timed out."));
		}
	}
}

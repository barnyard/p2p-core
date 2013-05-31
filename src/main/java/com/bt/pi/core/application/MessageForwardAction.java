package com.bt.pi.core.application;

import rice.p2p.commonapi.Id;

public class MessageForwardAction {
	private boolean shouldForward;
	private Id newDestinationNodeId;
	
	public MessageForwardAction(boolean aShouldForwardMessage) {
		this(aShouldForwardMessage, null);
	}
	
	public MessageForwardAction(boolean aShouldForwardMessage, Id aNewDestinationNodeId) {
		this.shouldForward = aShouldForwardMessage;
		this.newDestinationNodeId = aNewDestinationNodeId;
	}

	public boolean shouldForwardMessage() {
		return shouldForward;
	}

	public Id getNewDestinationNodeId() {
		return newDestinationNodeId;
	}
}

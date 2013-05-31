package com.bt.pi.core.node;

import org.springframework.context.ApplicationEvent;

public class NodeStartedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 8847278920842294631L;

    public NodeStartedEvent(Object source) {
        super(source);
    }
}

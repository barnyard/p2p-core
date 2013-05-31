package com.bt.pi.core.scribe;

import org.springframework.context.ApplicationEvent;

import rice.p2p.commonapi.Id;
import rice.p2p.scribe.ScribeContent;

import com.bt.pi.core.scribe.content.KoalaScribeContent;

public class AnycastFailureEvent extends ApplicationEvent {
    private static final long serialVersionUID = -4788197508019989942L;

    private Id topicId;
    private KoalaScribeContent koalaScribeContent;

    public AnycastFailureEvent(Object source) {
        super(source);
    }

    public AnycastFailureEvent(Object source, Id aTopicId, ScribeContent scribeContent) {
        this(source);
        topicId = aTopicId;
        if (scribeContent instanceof KoalaScribeContent)
            koalaScribeContent = (KoalaScribeContent) scribeContent;
        else
            throw new IllegalArgumentException("Scribe content not instance of KoalaScribeContent");
    }

    public Id getTopicId() {
        return topicId;
    }

    public KoalaScribeContent getKoalaScribeContent() {
        return koalaScribeContent;
    }
}

package com.bt.pi.core.application.activation;

import org.springframework.context.ApplicationEvent;

public class ApplicationRecordRefreshedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 4489506563218254835L;
    private transient ApplicationRecord applicationRecord;

    public ApplicationRecordRefreshedEvent(ApplicationRecord anApplicationRecord, Object source) {
        super(source);
        applicationRecord = anApplicationRecord;
    }

    public ApplicationRecord getApplicationRecord() {
        return applicationRecord;
    }
}

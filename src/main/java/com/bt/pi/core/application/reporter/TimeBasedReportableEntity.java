package com.bt.pi.core.application.reporter;

public abstract class TimeBasedReportableEntity<T> extends ReportableEntity<T> {
    public TimeBasedReportableEntity(String nodeId) {
        super(nodeId);

    }

    public TimeBasedReportableEntity() {

    }

    public abstract Object getId();
}

package com.bt.pi.core.application.health.entity;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.core.application.reporter.ReportableEntity;

public class LogMessageEntity extends ReportableEntity<LogMessageEntity> {
    private static final Log LOG = LogFactory.getLog(LogMessageEntity.class);
    private static final String S = "%s";

    private Date timestamp;
    private String logMessage;
    private String className;
    private String logTxId;

    public LogMessageEntity() {
        timestamp = new Date();
    }

    public LogMessageEntity(String nodeId) {
        super(nodeId);
        timestamp = new Date();
    }

    public LogMessageEntity(long aTimestamp, String aLogMessage, String aClassName, String aLogTxId, String aNodeId) {
        super(aNodeId);
        setTimestamp(DateFormat.getDateTimeInstance().format(new Date(aTimestamp)));
        logMessage = aLogMessage;
        className = aClassName;
        logTxId = aLogTxId;

    }

    public String getTimestamp() {
        return DateFormat.getDateTimeInstance().format(timestamp);
    }

    public void setTimestamp(String aTimestamp) {
        try {
            this.timestamp = DateFormat.getDateTimeInstance().parse(aTimestamp);
        } catch (ParseException e) {
            LOG.error(String.format("Unable to parse date: %s", aTimestamp), e);
        }
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String aLogMessage) {
        this.logMessage = aLogMessage;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String aClassName) {
        this.className = aClassName;
    }

    public String getLogTxId() {
        return logTxId;
    }

    public void setLogTxId(String aLogTxId) {
        this.logTxId = aLogTxId;
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @JsonIgnore
    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public int compareTo(LogMessageEntity anotherLogMessageEntity) {
        int result = timestamp.compareTo(anotherLogMessageEntity.timestamp);
        if (result != 0)
            return result;

        result = String.format(S, getNodeId()).compareTo(String.format(S, anotherLogMessageEntity.getNodeId()));
        if (result != 0)
            return result;

        result = String.format(S, className).compareTo(String.format(S, anotherLogMessageEntity.className));
        if (result != 0)
            return result;

        return String.format(S, logMessage).compareTo(String.format(S, anotherLogMessageEntity.logMessage));
    }

    @Override
    public String toString() {
        return String.format("[LogMessageEntity: [timeStamp: %s, logMessage: %s, className: %s, logTxId: %s, nodeId: %s]]", timestamp, logMessage, className, logTxId, getNodeId());
    }

    @Override
    public boolean equals(Object other) {
        if (null == other)
            return false;
        if (!(other instanceof LogMessageEntity))
            return false;
        LogMessageEntity otherEntity = (LogMessageEntity) other;

        return new EqualsBuilder().append(timestamp, otherEntity.timestamp).append(logMessage, otherEntity.logMessage).append(className, otherEntity.className).append(logTxId, otherEntity.logTxId).append(getNodeId(), otherEntity.getNodeId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(timestamp).append(logMessage).append(className).append(logTxId).append(getNodeId()).toHashCode();
    }

    @JsonIgnore
    @Override
    public Object[] getKeysForMap() {
        return new String[] { getNodeId() };
    }

    @JsonIgnore
    @Override
    public int getKeysForMapCount() {
        return 1;
    }

    @Override
    public long getCreationTime() {
        return timestamp.getTime();
    }

    @Override
    public String getUriScheme() {
        return this.getClass().getSimpleName();
    }
}

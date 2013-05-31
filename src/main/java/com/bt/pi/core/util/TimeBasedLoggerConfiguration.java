package com.bt.pi.core.util;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;

public class TimeBasedLoggerConfiguration<V> {
    public static final long DEFAULT_TIMEOUT = 100;

    public static final long DEFAULT_TIME_BETWEEN_LOGS = 10000;

    private boolean active;

    private long timeout = DEFAULT_TIMEOUT;

    private long timeBetweenLogs = DEFAULT_TIME_BETWEEN_LOGS;

    private Callable<V> callableToRun;

    private String logHeader;

    private Log log;

    public TimeBasedLoggerConfiguration(boolean isActive, Log alog, long aTimeOut, long aTimeBetweenLogs) {
        active = isActive;
        log = alog;
        timeout = aTimeOut;
        timeBetweenLogs = aTimeBetweenLogs;

    }

    public TimeBasedLoggerConfiguration(boolean isActive, Callable<V> aCallable, String alogHeader, Log alog) {
        active = isActive;
        callableToRun = aCallable;
        logHeader = alogHeader;
        log = alog;

    }

    public TimeBasedLoggerConfiguration(boolean isActive, Callable<V> aCallable, String alogHeader, Log alog, long aTimeOut, long aTimeBetweenLogs) {
        active = isActive;
        callableToRun = aCallable;
        logHeader = alogHeader;
        log = alog;
        timeout = aTimeOut;
        timeBetweenLogs = aTimeBetweenLogs;

    }

    public TimeBasedLoggerConfiguration() {
        callableToRun = null;
        log = null;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean anActive) {
        this.active = anActive;
    }

    public long getTimeout() {
        return timeout;
    }

    public long getTimeBetweenLogs() {
        return timeBetweenLogs;
    }

    public Callable<V> getCallableToRun() {
        return callableToRun;
    }

    public String getLogHeader() {
        return logHeader;
    }

    public Log getLog() {
        return log;
    }

    public void setTimeout(long atimeout) {
        this.timeout = atimeout;
    }

    public void setTimeBetweenLogs(long atimeBetweenLogs) {
        this.timeBetweenLogs = atimeBetweenLogs;
    }

    public void setCallableToRun(Callable<V> acallableToRun) {
        this.callableToRun = acallableToRun;
    }

    public void setLogHeader(String alogHeader) {
        this.logHeader = alogHeader;
    }

    public void setLog(Log alog) {
        this.log = alog;
    }

}

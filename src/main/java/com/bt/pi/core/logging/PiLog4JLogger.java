package com.bt.pi.core.logging;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.environment.logging.Logger;

public class PiLog4JLogger extends Logger {

    private Log logger;
    private String instanceName;
    private String instanceNameMessagePrefix;

    public PiLog4JLogger(@SuppressWarnings("rawtypes") Class loggingClass, String instance, String logLevel) {
        logger = LogFactory.getLog(loggingClass);
        level = convertLog4JLevelToPastryLevel(logLevel);
        instanceName = instance;
        instanceNameMessagePrefix = StringUtils.isBlank(instanceName) ? "" : String.format(" Instance - %s: ", instanceName);
    }

    protected int convertLog4JLevelToPastryLevel(String logLevel) {
        int pastryLogLevel = PiLog4JLogger.INFO;
        if (StringUtils.isNotBlank(logLevel)) {
            if (logLevel.equalsIgnoreCase("off")) {
                pastryLogLevel = PiLog4JLogger.OFF;
            } else if (logLevel.equalsIgnoreCase("fatal")) {
                pastryLogLevel = PiLog4JLogger.SEVERE;
            } else if (logLevel.equalsIgnoreCase("warn")) {
                pastryLogLevel = PiLog4JLogger.WARNING;
            } else if (logLevel.equalsIgnoreCase("debug")) {
                pastryLogLevel = PiLog4JLogger.FINE;
            } else if (logLevel.equalsIgnoreCase("trace")) {
                pastryLogLevel = PiLog4JLogger.FINEST;
            } else if (logLevel.equalsIgnoreCase("all")) {
                pastryLogLevel = PiLog4JLogger.ALL;
            }
        }
        return pastryLogLevel;
    }

    @Override
    public void log(String message) {
        Throwable t = new Throwable();
        StackTraceElement[] stackTraceArray = t.getStackTrace();
        String classname = null;
        int lineNumber = 0;
        StackTraceElement methodCaller = null;
        if (stackTraceArray != null && stackTraceArray.length > 1) {
            methodCaller = stackTraceArray[1];
            classname = methodCaller.getClassName();
            lineNumber = methodCaller.getLineNumber();
        }
        logger.debug(String.format("[%s %d]", classname, lineNumber) + instanceNameMessagePrefix + message);
    }

    @Override
    public void logException(String message, Throwable exception) {
        logger.error(instanceNameMessagePrefix + message, exception);
    }
}

package com.bt.pi.core.util;

import java.util.concurrent.Callable;

public class TimeBasedLogger<V> implements Callable<V> {

    public static final long DEFAULT_TIMEOUT = 100;

    public static final long DEFAULT_TIME_BETWEEN_LOGS = 10000;

    private TimeBasedLoggerConfiguration<V> conf;

    private volatile long lastUsed;

    public TimeBasedLogger(TimeBasedLoggerConfiguration<V> aConf) {
        conf = aConf;
    }

    @Override
    public V call() throws Exception {
        final long startTime = System.currentTimeMillis();
        try {
            V result = conf.getCallableToRun().call();
            return result;
        } finally {
            if (conf.isActive()) {
                long methodExecutionTime = System.currentTimeMillis() - startTime;
                if (executionTimeBiggerThanTimeOutAndShouldLog(startTime, methodExecutionTime)) {

                    lastUsed = System.currentTimeMillis();
                    logTime(conf.getLogHeader(), methodExecutionTime);

                }
            }
        }

    }

    private boolean executionTimeBiggerThanTimeOutAndShouldLog(final long startTime, long methodExecutionTime) {
        return methodExecutionTime > conf.getTimeout() && (lastUsed == 0 || (startTime - lastUsed) > conf.getTimeBetweenLogs());
    }

    private void logTime(String heading, long time) {
        conf.getLog().info(heading + " took: " + time + " millis");
    }

}

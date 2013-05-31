package com.bt.pi.core.testing;

import java.util.Collection;
import java.util.Properties;

import junit.framework.Assert;

public class LogHelper {

    private LogHelper() {
    }

    public static void assertTimeWithinReason(long expected, long actual, long wiggleRoom) {
        assertTimeWithinReason("Expected " + expected + ", actual " + actual + ", wiggle " + wiggleRoom, expected, actual, wiggleRoom);
    }

    public static void assertTimeWithinReason(String message, long expected, long actual, long wiggleRoom) {
        Assert.assertTrue(message, Math.abs(expected - actual) <= wiggleRoom);
    }

    public static void initLogging() {
        Properties p = new Properties();
        p.put("log4j.rootCategory", "WARN, stdout, LOGFILE");
        p.put("log4j.appender.LOGFILE", VectorAppender.class.getName());
        p.put("log4j.logger.com", "DEBUG");

        // For logging to console
        p.put("log4j.appender.stdout.Threshold", "DEBUG");
        p.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        p.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        p.put("log4j.appender.stdout.layout.ConversionPattern", "%d %-5p [%c{1}][%X{userId}][%X{transactionId}][%X{appCtx}] - %m%n");
        // p.put("log4j.logger.org", "DEBUG");
        org.apache.log4j.PropertyConfigurator.configure(p);

        resetLogging();
    }

    public static void resetLogging() {
        VectorAppender.clear();
    }

    public static boolean containsString(Collection<String> coll, String str) {
        for (String s : coll) {
            if (s != null && s.contains(str))
                return true;
        }
        return false;
    }
}

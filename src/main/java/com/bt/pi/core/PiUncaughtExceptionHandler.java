package com.bt.pi.core;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PiUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static final Log LOG = LogFactory.getLog(PiUncaughtExceptionHandler.class);

    public PiUncaughtExceptionHandler() {
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOG.error(String.format("Exception in thread %s", t.getName()), e);
        if (e instanceof VirtualMachineError) {
            LOG.fatal("!!!!!!!!!!!!!!!!!!!  Error caught: " + e.getMessage() + ", exiting PI !!!!!!!!!!!!!!!!!!");
            System.exit(-1);
        }
    }
}

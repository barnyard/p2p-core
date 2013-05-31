package com.bt.pi.core.node;

import java.util.concurrent.FutureTask;

public class PiApplicationFutureTask extends FutureTask<String> {
    private String applicationName;

    public PiApplicationFutureTask(Runnable runnable, String anApplicationName) {
        super(runnable, null);
        applicationName = anApplicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }
}

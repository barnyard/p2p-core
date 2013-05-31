package com.bt.pi.core.util;

import org.springframework.stereotype.Component;

@Component
public class InstrumentedLogIfSlowAspect extends LogIfSlowAspect {

    private TestLogIfSlowAspectListener listener;

    public InstrumentedLogIfSlowAspect() {

    }

    @Override
    protected void logTime(String heading, long time) {
        if (listener != null)
            listener.call(heading, time);
        super.logTime(heading, time);
    }

    public interface TestLogIfSlowAspectListener {
        void call(String heading, long time);
    }

    public TestLogIfSlowAspectListener getListener() {
        return listener;
    }

    public void setListener(TestLogIfSlowAspectListener alistener) {
        this.listener = alistener;
    }

}

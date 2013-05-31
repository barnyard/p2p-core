package com.bt.pi.core.testing;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class UnitTestAppender implements Appender {
    private List<String> messages = new Vector<String>();
    private int startInbound;
    private int endInbound;
    private int startOutbound;
    private int endOutbound;

    public UnitTestAppender() {
    }

    public List<String> getMessages() {
        return messages;
    }

    public int getStartInbound() {
        return startInbound;
    }

    public int getEndInbound() {
        return endInbound;
    }

    public int getStartOutbound() {
        return startOutbound;
    }

    public int getEndOutbound() {
        return endOutbound;
    }

    public void reset() {
        getMessages().clear();
        startInbound = 0;
        endInbound = 0;
        startOutbound = 0;
        endOutbound = 0;
    }

    public void addFilter(Filter arg0) {
    }

    public Filter getFilter() {
        return null;
    }

    public void clearFilters() {
    }

    public void close() {
    }

    public void doAppend(LoggingEvent arg0) {
        String message = arg0.getMessage().toString();
        if (message.contains("START INBOUND"))
            startInbound++;
        else if (message.contains("END INBOUND"))
            endInbound++;
        else if (message.contains("START OUTBOUND"))
            startOutbound++;
        else if (message.contains("END OUTBOUND"))
            endOutbound++;
        messages.add(message.toString());
    }

    public String getName() {
        return null;
    }

    public void setErrorHandler(ErrorHandler arg0) {
    }

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setLayout(Layout arg0) {
    }

    public Layout getLayout() {
        return null;
    }

    public void setName(String arg0) {
    }

    public boolean requiresLayout() {
        return false;
    }
}

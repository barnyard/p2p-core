package com.bt.pi.core.testing;

import java.util.Vector;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class VectorAppender extends AppenderSkeleton {
	private static Vector <String>messages = new Vector<String>();
    public static final boolean VECTORAPPENDER_SYSOUT = false;

    @Override
	protected void append(LoggingEvent arg0) {
	    if (VectorAppender.VECTORAPPENDER_SYSOUT) {
	        System.out.println(arg0.getLoggerName() + "  - " + arg0.getRenderedMessage());
        }
		messages.add(arg0.getLoggerName() + "  - " + arg0.getRenderedMessage());
	}
	public void close() {
		
	}
	public boolean requiresLayout() {
		return false;
	}
	public static void clear() {
		messages = new Vector<String>();
	}
	public static Vector<String> getMessages() {
		return messages;
	}
}

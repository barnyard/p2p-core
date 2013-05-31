package com.bt.pi.core.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.environment.exception.ExceptionStrategy;

@Component
public class PiExceptionStrategy implements ExceptionStrategy {
    private static final Log LOG = LogFactory.getLog(PiExceptionStrategy.class);

    public PiExceptionStrategy() {
    }

    @Override
    public void handleException(Object source, Throwable t) {
        LOG.error("Handling exception in selector thread from " + source, t);
        if (t instanceof Error)
            throw (Error) t;
    }

}

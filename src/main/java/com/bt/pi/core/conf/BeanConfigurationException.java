/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.conf;

import org.springframework.beans.BeansException;

/**
 * Thrown by {@link PropertyAnnotationAndPlaceholderConfigurer}.
 */
public class BeanConfigurationException extends BeansException {

    private static final long serialVersionUID = -7455805021926291404L;

    public BeanConfigurationException(String message) {
        super(message);
    }

}

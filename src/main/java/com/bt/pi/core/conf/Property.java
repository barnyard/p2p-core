/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.conf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add this annotation to a setter to have a {@link PropertyAnnotationAndPlaceholderConfigurer} call it.
 * 
 * From Ricardo Gladwell's answer at http://stackoverflow.com/questions/317687/inject-property-value-into-spring-bean.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Property {
    String key();

    String defaultValue() default "";

    boolean required() default true;
}

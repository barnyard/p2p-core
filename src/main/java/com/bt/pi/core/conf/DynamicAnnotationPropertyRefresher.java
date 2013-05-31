/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.conf;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;

public class DynamicAnnotationPropertyRefresher extends PropertyParseBase {
    private static final Log LOG = LogFactory.getLog(DynamicAnnotationPropertyRefresher.class);
    private static final String NO_SUCH_PROPERTY_S_FOR_CLASS_S_FOUND_IN_PROPERTIES = "No such property=[%s] for class %s found in properties";
    private static final String FOUND_PROPERTY_ANNOTATION_ON_PROPERTY_S_S = "found @Property annotation on property=[%s.%s]";
    private static final String EXAMINING_PROPERTY_S_S = "examining property=[%s.%s]";
    private static final String ERROR_INJECTING_PROPERTY = "Error injecting property";

    public DynamicAnnotationPropertyRefresher() {
    }

    private void processClass(String propertyName, String propertyValue, Class<?> clazz, Object bean) {
        for (PropertyDescriptor property : BeanUtils.getPropertyDescriptors(clazz)) {
            LOG.debug(String.format(EXAMINING_PROPERTY_S_S, clazz.getName(), property.getName()));
            Method setter = property.getWriteMethod();
            if (setter != null && setter.isAnnotationPresent(Property.class)) {
                processProperty(propertyName, propertyValue, clazz, property, setter, bean);
            }
        }
    }

    private void processProperty(String propertyName, String propertyValue, Class<?> clazz, PropertyDescriptor property, Method setter, Object bean) {
        LOG.debug(String.format(FOUND_PROPERTY_ANNOTATION_ON_PROPERTY_S_S, clazz.getName(), property.getName()));
        Property annotation = setter.getAnnotation(Property.class);
        if (!propertyName.equals(annotation.key())) {
            LOG.debug(String.format(NO_SUCH_PROPERTY_S_FOR_CLASS_S_FOUND_IN_PROPERTIES, annotation.key(), clazz.getSimpleName()));
            return;
        }

        try {
            Class<?> parameterType = setter.getParameterTypes()[0];
            if (setParameter(setter, bean, propertyValue, parameterType))
                return;
            LOG.error(String.format("unsupported parameter type %s for method %s", parameterType, setter.getName()));
        } catch (IllegalAccessException e) {
            LOG.error(ERROR_INJECTING_PROPERTY, e);
        } catch (InvocationTargetException e) {
            LOG.error(ERROR_INJECTING_PROPERTY, e);
        }
    }

    @Override
    protected void processProperty(String propertyName, String propertyValue) {
        for (String name : getApplicationContext().getBeanDefinitionNames()) {
            Class<?> clazz = getApplicationContext().getType(name);
            Object bean = getApplicationContext().getBean(name);

            if (clazz != null) {
                processClass(propertyName, propertyValue, clazz, bean);
            }
        }
    }
}
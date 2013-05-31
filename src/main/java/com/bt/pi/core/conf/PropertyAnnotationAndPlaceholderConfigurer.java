/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.conf;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Calls setters annotated with {@link Property} using values from a {@link Properties} instance.
 * 
 * From Ricardo Gladwell's answer at http://stackoverflow.com/questions/317687/inject-property-value-into-spring-bean.
 */
public class PropertyAnnotationAndPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static final Log LOG = LogFactory.getLog(PropertyAnnotationAndPlaceholderConfigurer.class);

    public PropertyAnnotationAndPlaceholderConfigurer() {
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties properties) {
        LOG.debug(String.format("Processing properties for beanFactory=[%s] with properties=[%s]", beanFactory, properties));
        super.processProperties(beanFactory, properties);

        for (String name : beanFactory.getBeanDefinitionNames()) {
            MutablePropertyValues mpv = beanFactory.getBeanDefinition(name).getPropertyValues();
            Class<?> clazz = beanFactory.getType(name);

            LOG.debug(String.format("Configuring properties for bean=%s[%s]", name, clazz));

            if (clazz != null) {
                processClass(properties, mpv, clazz);
            }
        }
    }

    private void processClass(Properties properties, MutablePropertyValues mpv, Class<?> clazz) {
        for (PropertyDescriptor property : BeanUtils.getPropertyDescriptors(clazz)) {
            LOG.debug(String.format("examining property=[%s.%s]", clazz.getName(), property.getName()));
            Method setter = property.getWriteMethod();
            if (setter != null && setter.isAnnotationPresent(Property.class)) {
                processProperty(properties, mpv, clazz, property, setter);
            }
        }
    }

    private void processProperty(Properties properties, MutablePropertyValues mpv, Class<?> clazz, PropertyDescriptor property, Method setter) {
        LOG.debug(String.format("found @Property annotation on property=[%s.%s]", clazz.getName(), property.getName()));
        Property annotation = setter.getAnnotation(Property.class);
        String value = resolvePlaceholder(annotation.key(), properties, SYSTEM_PROPERTIES_MODE_FALLBACK);
        if (StringUtils.isEmpty(value)) {
            value = annotation.defaultValue();
        }
        if (StringUtils.isEmpty(value) && annotation.required()) {
            throw new BeanConfigurationException(String.format("No such property=[%s] for class %s found in properties", annotation.key(), clazz.getSimpleName()));
        }
        LOG.debug(String.format("setting property=[%s.%s] value=[%s=%s]", clazz.getName(), property.getName(), annotation.key(), value));
        mpv.addPropertyValue(property.getName(), value);
    }
}
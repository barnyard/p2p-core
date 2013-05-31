package com.bt.pi.core.conf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

public abstract class PropertyParseBase implements ApplicationContextAware, ConfigurationListener {
    private static final Log LOG = LogFactory.getLog(PropertyParseBase.class);
    private static final int REFRESH_INTERVAL = 60000;

    private int refreshInterval = REFRESH_INTERVAL;
    private ApplicationContext applicationContext;
    private Resource[] locations;
    private List<PropertiesConfiguration> configurations = new ArrayList<PropertiesConfiguration>();
    private Map<String, String> propertyValueMap = new HashMap<String, String>();
    private PropertiesConfigurationLoader propertiesConfigurationLoader = new PropertiesConfigurationLoader();

    public PropertyParseBase() {
        applicationContext = null;
        locations = null;
    }

    @Override
    public void setApplicationContext(ApplicationContext anApplicationContext) {
        applicationContext = anApplicationContext;
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setRefreshInterval(int v) {
        this.refreshInterval = v;
    }

    public void setLocations(Resource[] theLocations) {
        this.locations = theLocations;
    }

    @PostConstruct
    public void setupConfiguration() {
        for (int i = 0; i < locations.length; i++) {
            if (!locations[i].exists()) {
                LOG.warn(String.format("unable to find configuration %s", locations[i].getDescription()));
                continue;
            }

            this.configurations.add(propertiesConfigurationLoader.loadConfig(locations[i], refreshInterval, this));
        }
    }

    @Override
    public void configurationChanged(ConfigurationEvent event) {
        LOG.debug(String.format("configurationChanged(%s)", event.getPropertyValue()));
        processProperties(ConfigurationConverter.getProperties((Configuration) event.getSource()));
    }

    private void processProperties(Properties aProperties) {
        for (Entry<Object, Object> entry : aProperties.entrySet()) {
            String propertyName = entry.getKey().toString();
            String propertyValue = entry.getValue().toString();

            if (propertyValueMap.containsKey(propertyName)) {
                String lastSavedPropertyValue = propertyValueMap.get(propertyName);
                if (propertyValue.equals(lastSavedPropertyValue)) {
                    LOG.debug(String.format("Not setting property[%s] as value [%s] is unchanged", propertyName, lastSavedPropertyValue));
                    continue;
                }
            }

            // add the property if it doesn't exist or changed
            propertyValueMap.put(propertyName, propertyValue);

            processProperty(propertyName, propertyValue);
        }
    }

    // force configurationChanged listener event by reading a value from each file every now and then
    @Scheduled(fixedDelay = REFRESH_INTERVAL)
    public void forceRefresh() {
        for (PropertiesConfiguration pc : configurations) {
            String key = (String) pc.getKeys().next();
            if (null != key) {
                LOG.debug(String.format("reading %s from %s to force update detection", key, pc.getURL()));
                pc.getString(key);
            }
        }
    }

    protected abstract void processProperty(String propertyName, String propertyValue);

    protected boolean setParameter(Method setter, Object bean, String value, Class<?> parameterType) throws IllegalAccessException, InvocationTargetException {
        if (parameterType.equals(String.class)) {
            setter.invoke(bean, value);
            return true;
        } else
            return setPrimitive(setter, bean, value, parameterType);
    }

    private boolean setPrimitive(Method setter, Object bean, String value, Class<?> parameterType) throws IllegalAccessException, InvocationTargetException {
        if (!parameterType.isPrimitive()) {
            return false;
        }
        if (parameterType.getName().equals("int")) {
            setter.invoke(bean, Integer.parseInt(value));
            return true;
        }
        if (parameterType.getName().equals("long")) {
            setter.invoke(bean, Long.parseLong(value));
            return true;
        }
        if (parameterType.getName().equals("boolean")) {
            setter.invoke(bean, Boolean.parseBoolean(value));
            return true;
        }
        if (parameterType.getName().equals("double")) {
            setter.invoke(bean, Double.parseDouble(value));
        }

        return false;
    }
}

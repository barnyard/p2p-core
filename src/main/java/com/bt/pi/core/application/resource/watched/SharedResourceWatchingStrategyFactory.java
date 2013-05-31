package com.bt.pi.core.application.resource.watched;

import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@SuppressWarnings("unchecked")
public class SharedResourceWatchingStrategyFactory implements ApplicationContextAware {
    private static final Log LOG = LogFactory.getLog(SharedResourceWatchingStrategyFactory.class);
    private Properties properties;
    private ApplicationContext applicationContext;

    public SharedResourceWatchingStrategyFactory() {
        properties = null;
        applicationContext = null;
    }

    @Resource
    public void setProperties(Properties aProperties) {
        this.properties = aProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext anApplicationContext) {
        applicationContext = anApplicationContext;
    }

    public SharedResourceWatchingStrategy<?> createWatchingStrategy(Class<?> entityClass) {
        LOG.debug(String.format("createWatchingStrategy(%s)", entityClass));
        WatchedResource watchedResource = entityClass.getAnnotation(WatchedResource.class);
        if (watchedResource == null)
            throw new WatchedResourceStrategyCreationException(String.format("%s is not a Watched Resource - please annotate as @WatchedResource", entityClass.getSimpleName()));

        Class<? extends SharedResourceWatchingStrategy> sharedResourceWatchingStrategyClass = watchedResource.watchingStrategy();
        SharedResourceWatchingStrategy<?> sharedResourceWatchingStrategy = instantiateStrategy(sharedResourceWatchingStrategyClass, entityClass);
        populateRefreshIntervalOverrides(watchedResource, entityClass, sharedResourceWatchingStrategy);
        populateConsumerWatcherIntervalOverrides(watchedResource, entityClass, sharedResourceWatchingStrategy);

        LOG.debug(String.format("Created watching strategy %s for entity %s", sharedResourceWatchingStrategy, entityClass));
        return sharedResourceWatchingStrategy;
    }

    private SharedResourceWatchingStrategy<?> instantiateStrategy(Class<? extends SharedResourceWatchingStrategy> sharedResourceWatchingStrategyClass, Class<?> entityClass) {
        LOG.debug(String.format("instantiateStrategy(%s, %s)", sharedResourceWatchingStrategyClass.getSimpleName(), entityClass.getSimpleName()));
        SharedResourceWatchingStrategy<?> sharedResourceWatchingStrategy;

        String[] beanNames = applicationContext.getBeanNamesForType((Class<?>) sharedResourceWatchingStrategyClass);
        if (beanNames.length != 1) {
            throw new WatchedResourceStrategyCreationException(String.format("Expected exactly one bean of type %s for watched resource entity %s", sharedResourceWatchingStrategyClass, entityClass));
        }

        String beanName = beanNames[0];
        if (applicationContext.isPrototype(beanName)) {
            sharedResourceWatchingStrategy = applicationContext.getBean(sharedResourceWatchingStrategyClass);
        } else {
            LOG.warn(String.format("Resource watching strategy %s has NOT been declared as a prototype bean - no dependency injection will take place", sharedResourceWatchingStrategyClass));
            try {
                sharedResourceWatchingStrategy = sharedResourceWatchingStrategyClass.newInstance();
            } catch (InstantiationException e) {
                throw new WatchedResourceStrategyCreationException(e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new WatchedResourceStrategyCreationException(e.getMessage(), e);
            }
        }
        return sharedResourceWatchingStrategy;
    }

    private void populateRefreshIntervalOverrides(WatchedResource watchedResource, Class<?> entityClass, SharedResourceWatchingStrategy<?> sharedResourceWatchingStrategy) {
        long initialResourceRefreshIntervalMillisOverride = watchedResource.defaultInitialResourceRefreshIntervalMillis();
        if (StringUtils.hasText(watchedResource.initialResourceRefreshIntervalMillisProperty()))
            initialResourceRefreshIntervalMillisOverride = parseLong(watchedResource.initialResourceRefreshIntervalMillisProperty(), initialResourceRefreshIntervalMillisOverride);
        if (initialResourceRefreshIntervalMillisOverride > 0) {
            sharedResourceWatchingStrategy.setInitialResourceRefreshIntervalMillis(initialResourceRefreshIntervalMillisOverride);
            LOG.debug(String.format("Overriding initial refresh interval for entity %s with value %d", entityClass, initialResourceRefreshIntervalMillisOverride));
        }

        long repeatingResourceRefreshIntervalMillisOverride = watchedResource.defaultRepeatingResourceRefreshIntervalMillis();
        if (StringUtils.hasText(watchedResource.repeatingResourceRefreshIntervalMillisProperty()))
            repeatingResourceRefreshIntervalMillisOverride = parseLong(watchedResource.repeatingResourceRefreshIntervalMillisProperty(), repeatingResourceRefreshIntervalMillisOverride);
        if (repeatingResourceRefreshIntervalMillisOverride > 0) {
            sharedResourceWatchingStrategy.setRepeatingResourceRefreshIntervalMillis(repeatingResourceRefreshIntervalMillisOverride);
            LOG.debug(String.format("Overriding repeating refresh interval for entity %s with value %d", entityClass, repeatingResourceRefreshIntervalMillisOverride));
        }
    }

    private void populateConsumerWatcherIntervalOverrides(WatchedResource watchedResource, Class<?> entityClass, SharedResourceWatchingStrategy<?> sharedResourceWatchingStrategy) {
        long initialConsumerWatcherIntervalMillisOverride = watchedResource.defaultInitialConsumerWatcherIntervalMillis();
        if (StringUtils.hasText(watchedResource.initialConsumerWatcherIntervalMillisProperty()))
            initialConsumerWatcherIntervalMillisOverride = parseLong(watchedResource.initialConsumerWatcherIntervalMillisProperty(), initialConsumerWatcherIntervalMillisOverride);
        if (initialConsumerWatcherIntervalMillisOverride > 0) {
            sharedResourceWatchingStrategy.setInitialConsumerWatcherIntervalMillis(initialConsumerWatcherIntervalMillisOverride);
            LOG.debug(String.format("Overriding initial consumer watcher interval for entity %s with value %d", entityClass, initialConsumerWatcherIntervalMillisOverride));
        }

        long repeatingConsumerWatcherIntervalMillisOverride = watchedResource.defaultRepeatingConsumerWatcherIntervalMillis();
        if (StringUtils.hasText(watchedResource.repeatingConsumerWatcherIntervalMillisProperty()))
            repeatingConsumerWatcherIntervalMillisOverride = parseLong(watchedResource.repeatingConsumerWatcherIntervalMillisProperty(), repeatingConsumerWatcherIntervalMillisOverride);
        if (repeatingConsumerWatcherIntervalMillisOverride > 0) {
            sharedResourceWatchingStrategy.setRepeatingConsumerWatcherIntervalMillis(repeatingConsumerWatcherIntervalMillisOverride);
            LOG.debug(String.format("Overriding repeating consumer watcher interval for entity %s with value %d", entityClass, repeatingConsumerWatcherIntervalMillisOverride));
        }
    }

    private long parseLong(String key, long defaultValue) {
        if (key == null)
            throw new WatchedResourceStrategyCreationException(String.format("Null property key"));

        String val = properties.getProperty(key);
        if (!StringUtils.hasText(val)) {
            LOG.debug(String.format("Property %s wasn't set, hence using default val %d", key, defaultValue));
            return defaultValue;
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new WatchedResourceStrategyCreationException(String.format("Invalid property key: '%s'", key));
        }
    }

}

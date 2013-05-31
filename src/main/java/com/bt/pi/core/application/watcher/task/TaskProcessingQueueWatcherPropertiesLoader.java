package com.bt.pi.core.application.watcher.task;

import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.bt.pi.core.conf.IllegalAnnotationException;
import com.bt.pi.core.conf.PropertyParseBase;

public class TaskProcessingQueueWatcherPropertiesLoader extends PropertyParseBase {
    private static final Log LOG = LogFactory.getLog(TaskProcessingQueueWatcherPropertiesLoader.class);

    private Properties properties;

    public TaskProcessingQueueWatcherPropertiesLoader() {
        properties = null;
    }

    @Resource
    public void setProperties(Properties aProperties) {
        properties = aProperties;
    }

    @PostConstruct
    public void loadProperties() {
        LOG.info("loadProperties()");
        Map<String, Object> beansWithAnnotatedProperties = getApplicationContext().getBeansWithAnnotation(TaskProcessingQueueWatcherProperties.class);
        for (Object bean : beansWithAnnotatedProperties.values()) {
            TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator = validateThatBeanIsTaskProcessingQueueWatcherInitiator(bean);
            TaskProcessingQueueWatcherProperties taskProcessingQueueWatcherProperties = bean.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

            populatePropertyOverrides(taskProcessingQueueWatcherProperties, taskProcessingQueueWatcherInitiator);
        }
    }

    @Override
    protected void processProperty(String propertyName, String propertyValue) {
        Map<String, Object> beansWithAnnotatedProperties = getApplicationContext().getBeansWithAnnotation(TaskProcessingQueueWatcherProperties.class);
        for (Object bean : beansWithAnnotatedProperties.values()) {
            TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator = validateThatBeanIsTaskProcessingQueueWatcherInitiator(bean);
            TaskProcessingQueueWatcherProperties taskProcessingQueueWatcherProperties = bean.getClass().getAnnotation(TaskProcessingQueueWatcherProperties.class);

            if (propertyName.equals(taskProcessingQueueWatcherProperties.staleQueueItemMillisProperty()))
                setStaleQueueItemMillis(taskProcessingQueueWatcherInitiator, (int) Long.parseLong(propertyValue));
            else if (propertyName.equals(taskProcessingQueueWatcherProperties.initialQueueWatcherIntervalMillisProperty()))
                setInitialQueueWatcherIntervalMillis(taskProcessingQueueWatcherInitiator, Long.parseLong(propertyValue));
            else if (propertyName.equals(taskProcessingQueueWatcherProperties.repeatingQueueWatcherIntervalMillisProperty()))
                setRepeatingQueueWatcherIntervalMillis(taskProcessingQueueWatcherInitiator, Long.parseLong(propertyValue));
        }
    }

    private void setRepeatingQueueWatcherIntervalMillis(TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator, long repeatingQueueWatcherIntervalMillis) {
        if (repeatingQueueWatcherIntervalMillis > 0) {
            taskProcessingQueueWatcherInitiator.setRepeatingQueueWatcherIntervalMillis(repeatingQueueWatcherIntervalMillis);
            LOG.debug(String.format("Overriding repeating queue watcher interval millis for entity %s with value %d", taskProcessingQueueWatcherInitiator.getClass(), repeatingQueueWatcherIntervalMillis));
        }
    }

    private void setInitialQueueWatcherIntervalMillis(TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator, long initialQueueWatcherIntervalMillis) {
        if (initialQueueWatcherIntervalMillis > 0) {
            taskProcessingQueueWatcherInitiator.setInitialQueueWatcherIntervalMillis(initialQueueWatcherIntervalMillis);
            LOG.debug(String.format("Overriding initial queue watcher interval millis for entity %s with value %d", taskProcessingQueueWatcherInitiator.getClass(), initialQueueWatcherIntervalMillis));
        }
    }

    private void setStaleQueueItemMillis(TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator, int staleQueueItemMillis) {
        if (staleQueueItemMillis > 0) {
            taskProcessingQueueWatcherInitiator.setStaleQueueItemMillis(staleQueueItemMillis);
            LOG.debug(String.format("Overriding stale queue item millis for entity %s with value %d", taskProcessingQueueWatcherInitiator.getClass(), staleQueueItemMillis));
        }
    }

    private TaskProcessingQueueWatcherInitiatorBase validateThatBeanIsTaskProcessingQueueWatcherInitiator(Object bean) {
        if (!(bean instanceof TaskProcessingQueueWatcherInitiatorBase)) {
            LOG.error(String.format("Bean %s annotated as a task processing queue watcher but does not extend base class", bean.getClass().getName()));
            throw new IllegalAnnotationException();
        }
        return (TaskProcessingQueueWatcherInitiatorBase) bean;
    }

    private void populatePropertyOverrides(TaskProcessingQueueWatcherProperties taskProcessingQueueWatcherProperties, TaskProcessingQueueWatcherInitiatorBase taskProcessingQueueWatcherInitiator) {
        int staleQueueItemMillisOverride = taskProcessingQueueWatcherProperties.staleQueueItemMillis();
        if (StringUtils.hasText(taskProcessingQueueWatcherProperties.staleQueueItemMillisProperty()))
            staleQueueItemMillisOverride = (int) parseLong(taskProcessingQueueWatcherProperties.staleQueueItemMillisProperty(), staleQueueItemMillisOverride);
        setStaleQueueItemMillis(taskProcessingQueueWatcherInitiator, staleQueueItemMillisOverride);

        long initialQueueWatcherIntervalMillisOverride = taskProcessingQueueWatcherProperties.initialQueueWatcherIntervalMillis();
        if (StringUtils.hasText(taskProcessingQueueWatcherProperties.initialQueueWatcherIntervalMillisProperty()))
            initialQueueWatcherIntervalMillisOverride = parseLong(taskProcessingQueueWatcherProperties.initialQueueWatcherIntervalMillisProperty(), initialQueueWatcherIntervalMillisOverride);
        setInitialQueueWatcherIntervalMillis(taskProcessingQueueWatcherInitiator, initialQueueWatcherIntervalMillisOverride);

        long repeatingQueueWatcherIntervalMillisOverride = taskProcessingQueueWatcherProperties.repeatingQueueWatcherIntervalMillis();
        if (StringUtils.hasText(taskProcessingQueueWatcherProperties.repeatingQueueWatcherIntervalMillisProperty()))
            repeatingQueueWatcherIntervalMillisOverride = parseLong(taskProcessingQueueWatcherProperties.repeatingQueueWatcherIntervalMillisProperty(), repeatingQueueWatcherIntervalMillisOverride);
        setRepeatingQueueWatcherIntervalMillis(taskProcessingQueueWatcherInitiator, repeatingQueueWatcherIntervalMillisOverride);
    }

    private long parseLong(String key, long defaultValue) {
        if (key == null)
            throw new IllegalArgumentException(String.format("Null property key"));

        String val = properties.getProperty(key);
        if (!StringUtils.hasText(val)) {
            LOG.debug(String.format("Property %s wasn't set, hence using default val %d", key, defaultValue));
            return defaultValue;
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid property key: '%s'", key));
        }
    }
}

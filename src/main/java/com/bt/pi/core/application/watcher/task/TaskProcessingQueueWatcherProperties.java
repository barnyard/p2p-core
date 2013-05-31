package com.bt.pi.core.application.watcher.task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines properties for handling queue tasks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TaskProcessingQueueWatcherProperties {
    int THIRTY_MINUTES = 30 * 60 * 1000;

    int THREE_MINUTES = 3 * 60 * 1000;

    /**
     * Effective delay before an item is stale and picked up by the watcher
     */
    int staleQueueItemMillis() default THIRTY_MINUTES;

    /**
     * Initial delay before watcher service kicks in to process stale tasks
     */
    long initialQueueWatcherIntervalMillis() default THREE_MINUTES;

    /**
     * Repeating delay before watcher service kicks in to retry stale tasks
     */
    long repeatingQueueWatcherIntervalMillis() default THREE_MINUTES;

    String staleQueueItemMillisProperty() default "";

    String initialQueueWatcherIntervalMillisProperty() default "";

    String repeatingQueueWatcherIntervalMillisProperty() default "";
}

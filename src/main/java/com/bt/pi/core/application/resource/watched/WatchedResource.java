package com.bt.pi.core.application.resource.watched;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bt.pi.core.application.resource.NoOpSharedResourceWatchingStrategy;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WatchedResource {
    @SuppressWarnings("unchecked")
    Class<? extends SharedResourceWatchingStrategy> watchingStrategy() default NoOpSharedResourceWatchingStrategy.class;

    long defaultInitialResourceRefreshIntervalMillis() default -1;

    long defaultRepeatingResourceRefreshIntervalMillis() default -1;

    long defaultInitialConsumerWatcherIntervalMillis() default -1;

    long defaultRepeatingConsumerWatcherIntervalMillis() default -1;

    String initialResourceRefreshIntervalMillisProperty() default "";

    String repeatingResourceRefreshIntervalMillisProperty() default "";

    String initialConsumerWatcherIntervalMillisProperty() default "";

    String repeatingConsumerWatcherIntervalMillisProperty() default "";
}

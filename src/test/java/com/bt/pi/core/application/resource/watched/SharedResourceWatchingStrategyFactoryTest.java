package com.bt.pi.core.application.resource.watched;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.bt.pi.core.application.resource.DefaultDhtResourceWatchingStrategy;
import com.bt.pi.core.application.resource.NoOpSharedResourceWatchingStrategy;

public class SharedResourceWatchingStrategyFactoryTest {
    private static final String PROTOTYPE_BEAN_NAME = "prototypeBeanName";
    private static final String NON_PROTOTYPE_BEAN_NAME = "nonPrototypeBeanName";
    private SharedResourceWatchingStrategyFactory sharedResourceWatchingStrategyFactory;
    private Properties properties;
    private ApplicationContext applicationContext;

    @WatchedResource
    public class DummyWatchedResource {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, initialResourceRefreshIntervalMillisProperty = "initial.refresh", repeatingResourceRefreshIntervalMillisProperty = "repeating.refresh", initialConsumerWatcherIntervalMillisProperty = "initial.consumer.watcher", repeatingConsumerWatcherIntervalMillisProperty = "repeating.consumer.watcher")
    public class DummyWatchedResourceWithNonDefaultWatchingStrategy {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = 21000, defaultRepeatingResourceRefreshIntervalMillis = 22000, defaultInitialConsumerWatcherIntervalMillis = 23000, defaultRepeatingConsumerWatcherIntervalMillis = 24000)
    public class DummyWatchedResourceWithDefaultIntervalValues {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, defaultInitialResourceRefreshIntervalMillis = 21000, defaultRepeatingResourceRefreshIntervalMillis = 22000, defaultInitialConsumerWatcherIntervalMillis = 23000, defaultRepeatingConsumerWatcherIntervalMillis = 24000, initialResourceRefreshIntervalMillisProperty = "initial.refresh.override", repeatingResourceRefreshIntervalMillisProperty = "repeating.refresh.override", initialConsumerWatcherIntervalMillisProperty = "initial.consumer.watcher.override", repeatingConsumerWatcherIntervalMillisProperty = "repeating.consumer.watcher.override")
    public class DummyWatchedResourceWithDefaultAndOverridenIntervalValues {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, initialResourceRefreshIntervalMillisProperty = "bad.property")
    public class DummyWatchedResourceWithBadIntervalProperty {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, initialResourceRefreshIntervalMillisProperty = "blank.property", defaultInitialResourceRefreshIntervalMillis = 33333, repeatingResourceRefreshIntervalMillisProperty = "blank.property", defaultRepeatingResourceRefreshIntervalMillis = 44444, initialConsumerWatcherIntervalMillisProperty = "blank.property", defaultInitialConsumerWatcherIntervalMillis = 55555, repeatingConsumerWatcherIntervalMillisProperty = "blank.property", defaultRepeatingConsumerWatcherIntervalMillis = 66666)
    public class DummyWatchedResourceWithBlankIntervalProperty {
    }

    @WatchedResource(watchingStrategy = DefaultDhtResourceWatchingStrategy.class, initialResourceRefreshIntervalMillisProperty = "unparseable")
    public class DummyWatchedResourceWithUnparseableIntervalProperty {
    }

    @Before
    public void before() {
        properties = new Properties();
        properties.put("initial.refresh", "30000");
        properties.put("repeating.refresh", "60000");
        properties.put("initial.consumer.watcher", "90000");
        properties.put("repeating.consumer.watcher", "120000");
        properties.put("initial.refresh.override", "31000");
        properties.put("repeating.refresh.override", "61000");
        properties.put("initial.consumer.watcher.override", "91000");
        properties.put("repeating.consumer.watcher.override", "121000");
        properties.put("blank.property", "");
        properties.put("unparseable", "abc");

        applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeanNamesForType(isA(Class.class))).thenReturn(new String[] { NON_PROTOTYPE_BEAN_NAME });
        when(applicationContext.getBeanNamesForType(DefaultDhtResourceWatchingStrategy.class)).thenReturn(new String[] { PROTOTYPE_BEAN_NAME });
        when(applicationContext.isPrototype(PROTOTYPE_BEAN_NAME)).thenReturn(true);
        when(applicationContext.getBean(DefaultDhtResourceWatchingStrategy.class)).thenReturn(new DefaultDhtResourceWatchingStrategy());

        sharedResourceWatchingStrategyFactory = new SharedResourceWatchingStrategyFactory();
        sharedResourceWatchingStrategyFactory.setProperties(properties);
        sharedResourceWatchingStrategyFactory.setApplicationContext(applicationContext);
    }

    @Test(expected = WatchedResourceStrategyCreationException.class)
    public void shouldThrowForUnannotatedEntity() {
        // act
        sharedResourceWatchingStrategyFactory.createWatchingStrategy(Object.class);
    }

    @Test(expected = WatchedResourceStrategyCreationException.class)
    public void shouldThrowForMultipleBeanDefs() {
        // setup
        when(applicationContext.getBeanNamesForType(isA(Class.class))).thenReturn(new String[] { NON_PROTOTYPE_BEAN_NAME, PROTOTYPE_BEAN_NAME });

        // act
        sharedResourceWatchingStrategyFactory.createWatchingStrategy(Object.class);
    }

    @Test
    public void shouldUseNoOpStrategyByDefault() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResource.class);

        // assert
        assertTrue(res instanceof NoOpSharedResourceWatchingStrategy);
    }

    @Test
    public void shouldUseNoSpecifiedStrategyIfOneGiven() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithNonDefaultWatchingStrategy.class);

        // assert
        assertTrue(res instanceof DefaultDhtResourceWatchingStrategy);
    }

    @Test
    public void shouldInjectInitialRefreshIntervalFromDefaultIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultIntervalValues.class);

        // assert
        assertEquals(21000, res.getInitialResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingRefreshIntervalFromDefaultIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultIntervalValues.class);

        // assert
        assertEquals(22000, res.getRepeatingResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectInitialConsumerWatcherIntervalFromDefaultIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultIntervalValues.class);

        // assert
        assertEquals(23000, res.getInitialConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingConsumerWatcherIntervalFromDefaultIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultIntervalValues.class);

        // assert
        assertEquals(24000, res.getRepeatingConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldInjectInitialRefreshIntervalFromPropertyIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithNonDefaultWatchingStrategy.class);

        // assert
        assertEquals(30000, res.getInitialResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingRefreshIntervalFromPropertyIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithNonDefaultWatchingStrategy.class);

        // assert
        assertEquals(60000, res.getRepeatingResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectInitialConsumerWatcherIntervalFromPropertyIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithNonDefaultWatchingStrategy.class);

        // assert
        assertEquals(90000, res.getInitialConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingConsumerWatcherIntervalFromPropertyIfSpecified() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithNonDefaultWatchingStrategy.class);

        // assert
        assertEquals(120000, res.getRepeatingConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldInjectInitialRefreshIntervalFromPropertyOverridingADefault() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultAndOverridenIntervalValues.class);

        // assert
        assertEquals(31000, res.getInitialResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingRefreshIntervalFromPropertyOverridingADefault() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultAndOverridenIntervalValues.class);

        // assert
        assertEquals(61000, res.getRepeatingResourceRefreshIntervalMillis());
    }

    @Test
    public void shouldInjectInitialConsumerWatcherIntervalFromPropertyOverridingADefault() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultAndOverridenIntervalValues.class);

        // assert
        assertEquals(91000, res.getInitialConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldInjectRepeatingConsumerWatcherIntervalFromPropertyOverridingADefault() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithDefaultAndOverridenIntervalValues.class);

        // assert
        assertEquals(121000, res.getRepeatingConsumerWatcherIntervalMillis());
    }

    @Test
    public void shouldIgnoreBlankIntervalProperty() {
        // act
        SharedResourceWatchingStrategy<?> res = sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithBlankIntervalProperty.class);

        // assert
        assertEquals(33333, res.getInitialResourceRefreshIntervalMillis());
        assertEquals(44444, res.getRepeatingResourceRefreshIntervalMillis());
        assertEquals(55555, res.getInitialConsumerWatcherIntervalMillis());
        assertEquals(66666, res.getRepeatingConsumerWatcherIntervalMillis());
    }

    @Test(expected = WatchedResourceStrategyCreationException.class)
    public void shouldThrowWhenInjectingNonParseableIntervalProperty() {
        // act
        sharedResourceWatchingStrategyFactory.createWatchingStrategy(DummyWatchedResourceWithUnparseableIntervalProperty.class);
    }
}

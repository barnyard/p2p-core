package com.bt.pi.core.application.watcher.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherInitiatorBase;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherProperties;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueWatcherPropertiesLoader;
import com.bt.pi.core.conf.IllegalAnnotationException;

@RunWith(MockitoJUnitRunner.class)
public class TaskProcessingQueueWatcherPropertiesLoaderTest {
    private Map<String, Object> beansMap = new HashMap<String, Object>();
    private MyBean1 myBean1 = new MyBean1();
    private MyBean2 myBean2 = new MyBean2();
    private MyBean3 myBean3 = new MyBean3();

    private File f;

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private Properties properties;
    @InjectMocks
    private TaskProcessingQueueWatcherPropertiesLoader annotatedPropertiesLoader = new TaskProcessingQueueWatcherPropertiesLoader();

    @Before
    public void setup() throws Exception {
        beansMap.put("myBean1", myBean1);
        beansMap.put("myBean2", myBean2);
        beansMap.put("myBean3", myBean3);
        when(applicationContext.getBeansWithAnnotation(TaskProcessingQueueWatcherProperties.class)).thenReturn(beansMap);

        when(properties.getProperty("test")).thenReturn("42");
        when(properties.getProperty("test2")).thenReturn("84");
        when(properties.getProperty("test3")).thenReturn("126");

        f = new File("/tmp/unittest.properties");
        f.deleteOnExit();
        List<String> lines = Arrays.asList(new String[] { "test=123", "test2=456", "test3=789" });
        FileUtils.writeLines(f, lines);
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteQuietly(f);
    }

    @Test
    public void testThatTheRightSettersAreInvoked() throws Exception {
        // act
        annotatedPropertiesLoader.loadProperties();

        // assert
        assertThat(myBean1.getStaleQueueItemMillis(), equalTo(42));
        assertThat(myBean1.getInitialQueueWatcherIntervalMillis(), equalTo(84L));
        assertThat(myBean1.getRepeatingQueueWatcherIntervalMillis(), equalTo(126L));

        assertThat(myBean2.getStaleQueueItemMillis(), equalTo(1));
        assertThat(myBean2.getInitialQueueWatcherIntervalMillis(), equalTo(2L));
        assertThat(myBean2.getRepeatingQueueWatcherIntervalMillis(), equalTo(3L));

        assertThat(myBean3.getStaleQueueItemMillis(), equalTo(1800000));
        assertThat(myBean3.getInitialQueueWatcherIntervalMillis(), equalTo(180000L));
        assertThat(myBean3.getRepeatingQueueWatcherIntervalMillis(), equalTo(180000L));
    }

    @Test
    public void testPropertyIsRefreshedIfPropertiesFileIsUpdatedWithFileApplicationContext() throws Exception {
        // setup
        annotatedPropertiesLoader.setLocations(new Resource[] { new FileSystemResource(f) });
        annotatedPropertiesLoader.setRefreshInterval(10);
        annotatedPropertiesLoader.setupConfiguration();
        annotatedPropertiesLoader.loadProperties();
        assertThat(myBean1.getStaleQueueItemMillis(), equalTo(42));
        assertThat(myBean1.getInitialQueueWatcherIntervalMillis(), equalTo(84L));
        assertThat(myBean1.getRepeatingQueueWatcherIntervalMillis(), equalTo(126L));

        // act
        Thread.sleep(1010);
        updateFileContents();
        annotatedPropertiesLoader.forceRefresh();

        assertThat(myBean1.getStaleQueueItemMillis(), equalTo(123));
        assertThat(myBean1.getInitialQueueWatcherIntervalMillis(), equalTo(456L));
        assertThat(myBean1.getRepeatingQueueWatcherIntervalMillis(), equalTo(789L));
    }

    private void updateFileContents() throws Exception {
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=true", "propKey=propValue", "longValue=888", "doubleValue=27.1" }));
    }

    @Test(expected = IllegalAnnotationException.class)
    public void beanShouldExtendBaseClass() throws Exception {
        // setup
        beansMap.put("myBean4", new MyBean4());

        // act
        annotatedPropertiesLoader.loadProperties();
    }

    @TaskProcessingQueueWatcherProperties(staleQueueItemMillisProperty = "test", initialQueueWatcherIntervalMillisProperty = "test2", repeatingQueueWatcherIntervalMillisProperty = "test3")
    public class MyBean1 extends TaskProcessingQueueWatcherInitiatorBase {
        public MyBean1() {
            super(null, null);
        }
    }

    @TaskProcessingQueueWatcherProperties(staleQueueItemMillisProperty = "x", initialQueueWatcherIntervalMillisProperty = "y", repeatingQueueWatcherIntervalMillisProperty = "z", staleQueueItemMillis = 1, initialQueueWatcherIntervalMillis = 2, repeatingQueueWatcherIntervalMillis = 3)
    public class MyBean2 extends TaskProcessingQueueWatcherInitiatorBase {
        public MyBean2() {
            super(null, null);
        }
    }

    @TaskProcessingQueueWatcherProperties
    public class MyBean3 extends TaskProcessingQueueWatcherInitiatorBase {
        public MyBean3() {
            super(null, null);
        }
    }

    @TaskProcessingQueueWatcherProperties
    public class MyBean4 {
    }
}

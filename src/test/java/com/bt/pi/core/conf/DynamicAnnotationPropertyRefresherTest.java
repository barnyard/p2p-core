package com.bt.pi.core.conf;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/*
 * these tests use a delay of 1 second because File.lastModified only seems to have a 1 second granularity (at least on my pc)
 */
public class DynamicAnnotationPropertyRefresherTest {
    private File f = new File("/tmp/unittest.properties");

    @Before
    public void before() throws Exception {
        f.deleteOnExit();
        List<String> lines = Arrays.asList(new String[] { "intValue=123", "booleanValue=true", "propKey=propValue", "longValue=456", "doubleValue=25.1" });
        FileUtils.writeLines(f, lines);
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteQuietly(f);
    }

    @Test
    public void testPropertyIsRefreshedIfPropertiesFileIsUpdatedWithStaticApplicationContext() throws Exception {
        // setup
        StaticApplicationContext staticApplicationContext = new StaticApplicationContext();

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(ExampleBean.class);
        staticApplicationContext.registerBeanDefinition("exampleBean", beanDefinition);

        GenericBeanDefinition beanDefinition1 = new GenericBeanDefinition();
        beanDefinition1.setBeanClass(DynamicAnnotationPropertyRefresher.class);
        staticApplicationContext.registerBeanDefinition("configurer", beanDefinition1);

        DynamicAnnotationPropertyRefresher configurer = (DynamicAnnotationPropertyRefresher) staticApplicationContext.getBean("configurer");
        configurer.setLocations(new Resource[] { new FileSystemResource(f) });
        configurer.setRefreshInterval(10);
        configurer.setupConfiguration();
        configurer.setApplicationContext(staticApplicationContext);

        staticApplicationContext.refresh();

        Thread.sleep(1010);
        FileUtils.touch(f);
        configurer.forceRefresh();

        ExampleBean exampleBean = (ExampleBean) staticApplicationContext.getBean("exampleBean");
        assertEquals(123, exampleBean.getIntProp());
        assertEquals(456, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals(25.1, exampleBean.getDoubleProp(), 0.1);
        assertEquals("propValue", exampleBean.getAnnotated());

        // act
        Thread.sleep(1010);
        updateFileContents();
        configurer.forceRefresh();

        // assert
        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(27.1, exampleBean.getDoubleProp(), 0.1);
    }

    private void updateFileContents() throws Exception {
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=true", "propKey=propValue", "longValue=888", "doubleValue=27.1" }));
    }

    @Test
    public void testPropertyIsRefreshedIfPropertiesFileIsUpdatedWithFileApplicationContext() throws Exception {
        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("com/bt/pi/core/conf/DynamicAnnotationPropertyRefresherTest-context.xml");
        DynamicAnnotationPropertyRefresher configurer = (DynamicAnnotationPropertyRefresher) applicationContext.getBean("configurer");
        ExampleBean exampleBean = (ExampleBean) applicationContext.getBean("exampleBean");

        Thread.sleep(1010);
        FileUtils.touch(f);
        configurer.forceRefresh();

        assertEquals(123, exampleBean.getIntProp());
        assertEquals(456, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());

        Thread.sleep(1010);
        updateFileContents();
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());

    }

    @Test
    public void shouldNotUpdatePropertyIfValueIsNotChanged() throws Exception {
        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("com/bt/pi/core/conf/DynamicAnnotationPropertyRefresherTest-context.xml");
        DynamicAnnotationPropertyRefresher configurer = (DynamicAnnotationPropertyRefresher) applicationContext.getBean("configurer");
        ExampleBean exampleBean = (ExampleBean) applicationContext.getBean("exampleBean");

        Thread.sleep(1010);
        FileUtils.touch(f);
        configurer.forceRefresh();

        assertEquals(123, exampleBean.getIntProp());
        assertEquals(456, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());

        exampleBean.setBooleanProp(false);

        Thread.sleep(1010);
        updateFileContents();
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(false, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
    }

    @Test
    public void shouldAddNewPropertyAndRetainItsValueIfItIsChanged() throws Exception {
        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("com/bt/pi/core/conf/DynamicAnnotationPropertyRefresherTest-context.xml");
        DynamicAnnotationPropertyRefresher configurer = (DynamicAnnotationPropertyRefresher) applicationContext.getBean("configurer");
        ExampleBean exampleBean = (ExampleBean) applicationContext.getBean("exampleBean");

        Thread.sleep(1010);
        FileUtils.touch(f);
        configurer.forceRefresh();

        assertEquals(123, exampleBean.getIntProp());
        assertEquals(456, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(100, exampleBean.getNewIntProp());

        Thread.sleep(1010);
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=true", "propKey=propValue", "longValue=888", "newIntValue=999" }));
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(999, exampleBean.getNewIntProp());

        exampleBean.setNewInt(1000);

        Thread.sleep(1010);
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=false", "propKey=propValue", "longValue=888", "newIntValue=999" }));
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(false, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(1000, exampleBean.getNewIntProp());

        exampleBean.setBooleanProp(true);

        Thread.sleep(1010);
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=false", "propKey=propValue", "longValue=888", "newIntValue=999" }));
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(1000, exampleBean.getNewIntProp());
    }

    @Test
    public void shouldRetainItsValueIfPropertyIsRemovedFromFile() throws Exception {
        AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext("com/bt/pi/core/conf/DynamicAnnotationPropertyRefresherTest-context.xml");
        DynamicAnnotationPropertyRefresher configurer = (DynamicAnnotationPropertyRefresher) applicationContext.getBean("configurer");
        ExampleBean exampleBean = (ExampleBean) applicationContext.getBean("exampleBean");

        assertEquals(123, exampleBean.getIntProp());
        assertEquals(456, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(100, exampleBean.getNewIntProp());

        Thread.sleep(1010);
        FileUtils.writeLines(f, Arrays.asList(new String[] { "intValue=444", "booleanValue=true", "propKey=propValue", "longValue=888" }));
        configurer.forceRefresh();

        assertEquals(444, exampleBean.getIntProp());
        assertEquals(888, exampleBean.getLongProp());
        assertEquals(true, exampleBean.getBooleanProp());
        assertEquals("propValue", exampleBean.getAnnotated());
        assertEquals(100, exampleBean.getNewIntProp());
    }
}

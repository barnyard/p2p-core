package com.bt.pi.core.conf;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
public class PropertyAnnotationAndPlaceholderConfigurerIntegrationTest extends AbstractJUnit4SpringContextTests {

    private ExampleBean exampleBean;

    @Before
    public void before() {
        exampleBean = (ExampleBean) applicationContext.getBean("exampleBean");
    }

    @Test
    public void unannotatedPropertyThatHasNotBeenSetShouldBeNull() {
        assertEquals(null, exampleBean.getUnannotated());
    }

    @Test
    public void annotatedPropertyWithKeyFoundInPropertiesShouldHaveValueFromProperties() {
        assertEquals("propValue", exampleBean.getAnnotated());
    }

    @Test
    public void annotatedPropertyWithKeyNotFoundInPropertiesShouldHaveDefaultValueFromAnnotation() {
        assertEquals("defaultValue", exampleBean.getAnnotatedButNoPropertyDefined());
    }

    @Test
    public void keyNotFoundWithNoDefaultValueIsOkayIfRequiredIsFalse() {
        assertEquals(null, exampleBean.getUnrequiredProperty());
    }

    @Test
    public void shouldSetIntProperty() {
        assertEquals(123, exampleBean.getIntProp());
    }

    @Test
    public void shouldSetLongProperty() {
        assertEquals(456L, exampleBean.getLongProp());
    }

    @Test
    public void shouldSetBooleanProperty() {
        assertEquals(true, exampleBean.getBooleanProp());
    }

    @Test
    public void shouldSetDoubleProperty() {
        assertEquals(27.1, exampleBean.getDoubleProp(), 0.1);
    }

    // Note that this test does NOT use the same config or bean as the others
    @Test(expected = BeanConfigurationException.class)
    public void annotatedProperyWithKeyNotFoundInPropertiesAndNoDefaultValueShouldThrowException() {
        PropertyAnnotationAndPlaceholderConfigurer configurer = new PropertyAnnotationAndPlaceholderConfigurer();

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.addBeanFactoryPostProcessor(configurer);

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(BrokenBean.class);
        applicationContext.registerBeanDefinition("brokenBean", beanDefinition);

        configurer.processProperties(applicationContext.getBeanFactory(), new Properties());
    }

    public static class BrokenBean {
        private String annotatedButNoPropertyDefinedAndNoDefaultValue;

        @Property(key = "missing")
        public void setAnnotatedButNoPropertyDefinedAndNoDefaultValue(String annotatedButNoPropertyDefinedAndNoDefaultValue) {
            this.annotatedButNoPropertyDefinedAndNoDefaultValue = annotatedButNoPropertyDefinedAndNoDefaultValue;
        }

        public String getAnnotatedButNoPropertyDefinedAndNoDefaultValue() {
            return annotatedButNoPropertyDefinedAndNoDefaultValue;
        }
    }
}

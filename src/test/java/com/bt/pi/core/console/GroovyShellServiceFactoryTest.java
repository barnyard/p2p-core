package com.bt.pi.core.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.iterative.groovy.service.GroovyShellService;

public class GroovyShellServiceFactoryTest {

    private GroovyShellService shellService;
    private GroovyShellServiceFactory factory;

    @Before
    public void before() {
        shellService = mock(GroovyShellService.class);
        factory = new GroovyShellServiceFactory(shellService);
    }

    @Test
    public void shouldPassPortToShellServiceAsSocket() {
        factory.setPort(1234);
        verify(shellService).setSocket(1234);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void initializeShouldSetBindings() {
        factory.initialize();
        verify(shellService).setBindings(isA(Map.class));
    }

    @Test
    public void initializeShouldSetLaunchAtStart() {
        factory.initialize();
        verify(shellService).setLaunchAtStart(true);
    }

    @Test
    public void initializeShouldInitializeShellService() {
        factory.initialize();
        verify(shellService).initialize();
    }

    @Test
    public void destroyShouldDestroyShellService() {
        factory.destroy();
        verify(shellService).destroy();
    }

    @Test
    public void getObjectShouldReturnShellService() throws Exception {
        assertEquals(shellService, factory.getObject());
    }

    @Test
    public void getObjectTypeShouldReturnShellServiceClass() throws Exception {
        assertEquals(GroovyShellService.class, factory.getObjectType());
    }

    @Test
    public void shouldReturnTrueForIsSingleton() throws Exception {
        assertTrue(factory.isSingleton());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setApplicationContextShouldPutAllBeansIntoAMapBoundToAppCxt() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Object foo = new Object();
        Object bar = new Object();
        Object prototype = new Object();

        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[] { "foo", "bar", "prototype" });
        when(applicationContext.getBean("foo")).thenReturn(foo);
        when(applicationContext.getBean("bar")).thenReturn(bar);
        when(applicationContext.getBean("prototype")).thenReturn(prototype);

        when(applicationContext.isPrototype("prototype")).thenReturn(true);

        factory.setApplicationContext(applicationContext);
        factory.initialize();

        Map<String, Object> springMap = (Map<String, Object>) factory.getBindings().get("appCtx");
        assertEquals(2, springMap.size());
        assertEquals(foo, springMap.get("foo"));
        assertEquals(bar, springMap.get("bar"));
    }
}

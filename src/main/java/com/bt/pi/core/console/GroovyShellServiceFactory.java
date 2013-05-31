package com.bt.pi.core.console;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.iterative.groovy.service.GroovyShellService;

/**
 * FactoryBean to create GroovyShellService which has a variable <i>appCtx</i> bound to the Spring Beans.
 * 
 * This allows users to access Spring beans with appCxt['beanName'] from the console.
 */
@Component
public class GroovyShellServiceFactory implements FactoryBean<GroovyShellService>, ApplicationContextAware {
    private static final Log LOG = LogFactory.getLog(GroovyShellServiceFactory.class);
    private final GroovyShellService groovyShellService;
    private final Map<String, Object> bindings = new HashMap<String, Object>();

    public GroovyShellServiceFactory() {
        groovyShellService = new GroovyShellService();
    }

    // to allow testing with a mock
    GroovyShellServiceFactory(GroovyShellService mockGroovyShellService) {
        this.groovyShellService = mockGroovyShellService;
    }

    @Value("#{properties['console.port']}")
    public void setPort(int port) {
        groovyShellService.setSocket(port);
    }

    @PostConstruct
    public void initialize() {
        groovyShellService.setBindings(bindings);
        groovyShellService.setLaunchAtStart(true);
        groovyShellService.initialize();
    }

    @PreDestroy
    public void destroy() {
        groovyShellService.destroy();
    }

    // ApplicationContextAware methods

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        Map<String, Object> springBeans = new HashMap<String, Object>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            // Ignore prototype beans as instantiating them here forces dependency injection, which can cause
            // issues in downstream projects that define prototype beans with dependencies that are not satisfied
            // locally
            if (applicationContext.isPrototype(beanName)) {
                LOG.debug(String.format("Ignoring prototype bean %s", beanName));
            } else {
                springBeans.put(beanName, applicationContext.getBean(beanName));
            }
        }
        bindings.put("appCtx", springBeans);
    }

    // FactoryBean methods

    @Override
    public GroovyShellService getObject() throws Exception {
        return groovyShellService;
    }

    @Override
    public Class<? extends GroovyShellService> getObjectType() {
        return GroovyShellService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    // to allow inspection during tests
    Map<String, Object> getBindings() {
        return bindings;
    }
}

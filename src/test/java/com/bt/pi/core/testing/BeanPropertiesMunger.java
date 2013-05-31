package com.bt.pi.core.testing;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.bt.pi.core.application.health.ErrorLogAppender;
import com.bt.pi.core.application.health.LogMessageHandler;
import com.bt.pi.core.application.health.NodePhysicalHealthChecker;
import com.bt.pi.core.application.health.NodePhysicalHealthHandler;
import com.bt.pi.core.console.GroovyShellServiceFactory;
import com.bt.pi.core.environment.KoalaParameters;

public class BeanPropertiesMunger implements BeanPostProcessor {
    private static boolean doMunging = false;

    private static int healthPublishIntervalSize = -1;
    private static int healthBroadcastSize = -1;
    private static int logMessagePublishIntervalSize = -1;
    private static int logMessageBroadcastSize = -1;
    private static int logMessageKeepCount = -1;
    private static int groovyPort;
    private static boolean isFirstEverNode = true;

    public static void setDoMunging(boolean value) {
        doMunging = value;
    }

    public static void setHealthPublishIntervalAndBroadcastSizes(int aHealthPublishIntervalSize, int aHealthBroadcastSize) {
        healthPublishIntervalSize = aHealthPublishIntervalSize;
        healthBroadcastSize = aHealthBroadcastSize;
    }

    public static void resetHealthPublishIntervalAndBroadcastSizes() {
        healthPublishIntervalSize = -1;
        healthBroadcastSize = -1;
    }

    public static void setConsolePort(int p) {
        groovyPort = p;
    }

    public static void setLogMessagePublishIntervalAndBroadcastSizes(int aLogMessagePublishIntervalSize, int aLogMessageBroadcastSize) {
        logMessagePublishIntervalSize = aLogMessagePublishIntervalSize;
        logMessageBroadcastSize = aLogMessageBroadcastSize;
    }

    public static void resetLogMessagePublishIntervalAndBroadcastSizes() {
        logMessagePublishIntervalSize = -1;
        logMessageBroadcastSize = -1;
    }

    public static void setLogMessageKeepCount(int keepCount) {
        logMessageKeepCount = keepCount;
    }

    public static void resetLogMessageKeepCount() {
        logMessageKeepCount = -1;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!doMunging)
            return bean;

        if (beanName.equals("nodePhysicalHealthHandler")) {
            if (healthBroadcastSize != -1)
                ((NodePhysicalHealthHandler) bean).setBroadcastWindowSize(healthBroadcastSize);
            if (healthPublishIntervalSize != -1)
                ((NodePhysicalHealthHandler) bean).setPublishIntervalSeconds(healthPublishIntervalSize);
        } else if (beanName.equals("koalaParameters")) {
            ((KoalaParameters) bean).setString("can_start_new_ring", String.format("%s", isFirstEverNode));
            if (isFirstEverNode)
                isFirstEverNode = false;
        } else if (beanName.equals("nodePhysicalHealthChecker")) {
            if (healthPublishIntervalSize != -1)
                ((NodePhysicalHealthChecker) bean).setPublishIntervalSeconds(healthPublishIntervalSize);
        } else if (beanName.equals("errorLogAppender")) {
            if (logMessagePublishIntervalSize != -1)
                ((ErrorLogAppender) bean).setPublishIntervalSeconds(logMessagePublishIntervalSize);
        } else if (beanName.equals("logMessageHandler")) {
            if (logMessageBroadcastSize != -1)
                ((LogMessageHandler) bean).setBroadcastWindowSize(logMessageBroadcastSize);
            if (logMessagePublishIntervalSize != -1)
                ((LogMessageHandler) bean).setPublishIntervalSeconds(logMessagePublishIntervalSize);
            if (logMessageKeepCount != -1)
                ((LogMessageHandler) bean).setKeepCount(logMessageKeepCount);
        } else if (beanName.equals("groovyShellServiceFactory")) {
            System.err.println("Setting console port to " + groovyPort);
            ((GroovyShellServiceFactory) bean).setPort(groovyPort);
        }
        return bean;
    }
}

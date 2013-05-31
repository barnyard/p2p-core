package com.bt.pi.core;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "org.apache.commons.logging.*", "org.apache.log4j.*" })
@PrepareForTest({ Main.class, Thread.class })
public class MainTest {
    @Mock
    private ContextDiscoverer contextDiscoverer;
    @Mock
    private ClassPathXmlApplicationContext applicationContext;
    @Mock
    private CloudPlatform cloudPlatform;

    @Before
    public void before() throws Exception {
        PowerMockito.whenNew(ContextDiscoverer.class).withNoArguments().thenReturn(contextDiscoverer);
        when(contextDiscoverer.findPiContexts()).thenReturn("fred");
        PowerMockito.whenNew(ClassPathXmlApplicationContext.class).withParameterTypes(String[].class).withArguments(isA(String[].class)).thenReturn(applicationContext);
        when(applicationContext.getBean("cloudPlatform")).thenReturn(cloudPlatform);
    }

    @Test
    public void testMainSetsThreadUncaughtExceptionHandler() throws Exception {
        // setup
        PowerMockito.mockStatic(Thread.class);

        // act
        Main.main(new String[] {});

        // assert
        PowerMockito.verifyStatic();
        Thread.setDefaultUncaughtExceptionHandler(isA(UncaughtExceptionHandler.class));
    }

    @Test
    public void testThatExceptionFromCloudPlatformCauseSystemExit() throws Exception {
        // setup
        doThrow(new RuntimeException("poo")).when(cloudPlatform).start(isA(CommandLine.class));
        PowerMockito.mockStatic(System.class);

        // act
        Main.main(new String[] {});

        // assert
        PowerMockito.verifyStatic();
        System.exit(1);
    }
}

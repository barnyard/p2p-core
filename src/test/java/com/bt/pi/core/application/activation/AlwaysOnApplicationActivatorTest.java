package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.core.application.EchoApplication;

@RunWith(MockitoJUnitRunner.class)
public class AlwaysOnApplicationActivatorTest {
    private static final Long START_WAIT_TIMEOUT = 1000L;
    @InjectMocks
    private AlwaysOnApplicationActivator applicationActivator = new AlwaysOnApplicationActivator() {
        @Override
        protected void executeApplicationActivation(ActivationAwareApplication application) {
            super.executeApplicationActivation(application);
            executingActivationLatch.countDown();
        }
    };
    private ScheduledExecutorService scheduledExecutor;
    private ThreadPoolTaskExecutor executor;
    private ApplicationRegistry applicationRegistry;
    private EchoApplication echoApp;
    private CountDownLatch executingActivationLatch;
    private CountDownLatch applicationActiveLatch;
    @Mock
    private TimerTask cleanupTimerTask;

    @Before
    public void before() {

        // signalling
        executingActivationLatch = new CountDownLatch(1);
        applicationActiveLatch = new CountDownLatch(1);

        // deps
        applicationRegistry = new ApplicationRegistry() {
            @Override
            public synchronized void setApplicationStatus(String applicationName, ApplicationStatus status) {
                super.setApplicationStatus(applicationName, status);
                if (status == ApplicationStatus.ACTIVE)
                    applicationActiveLatch.countDown();
            }
        };
        scheduledExecutor = Executors.newScheduledThreadPool(4);
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        // app
        List<String> emptyList = Collections.emptyList();
        echoApp = mock(EchoApplication.class);
        when(echoApp.getApplicationName()).thenReturn("echo-app");
        when(echoApp.getPreferablyExcludedApplications()).thenReturn(emptyList);
        when(echoApp.getActivationCheckPeriodSecs()).thenReturn(60);
        when(echoApp.getStartTimeout()).thenReturn(START_WAIT_TIMEOUT);
        when(echoApp.getStartTimeoutUnit()).thenReturn(TimeUnit.MILLISECONDS);
        when(echoApp.becomeActive()).thenReturn(true);

        applicationActivator.setScheduledExecutorService(scheduledExecutor);
        applicationActivator.setExecutor(executor);
        applicationActivator.setApplicationRegistry(applicationRegistry);
    }

    @Test
    public void shouldPassLocalChecks() {
        // act
        ApplicationActivationCheckStatus res = applicationActivator.checkLocalActivationPreconditions(echoApp);

        // assert
        assertEquals(ApplicationActivationCheckStatus.ACTIVATE, res);
    }

    @Test
    public void shouldJustExecuteActivatorWhenAsked() throws Exception {
        // act
        applicationActivator.checkAndActivate(echoApp, cleanupTimerTask);

        // assert
        assertTrue(executingActivationLatch.await(5, TimeUnit.SECONDS));
        verify(cleanupTimerTask).cancel();
    }

    @Test
    public void shouldStartOnRegister() throws Exception {
        // act
        applicationActivator.register(echoApp);

        // assert
        assertTrue(applicationActiveLatch.await(5, TimeUnit.SECONDS));
        verify(echoApp).becomeActive();
        assertEquals(ApplicationStatus.ACTIVE, applicationRegistry.getApplicationStatus(echoApp.getApplicationName()));
    }

    @Test(expected = NotImplementedException.class)
    public void shouldThrowExceptionWhennDeActivateNodeCalled() {
        applicationActivator.deActivateNode(null, null);
    }

    @Test
    public void heartbeatShouldNoOp() {
        applicationActivator.checkActiveApplicationStillActiveAndHeartbeat(echoApp);
    }
}

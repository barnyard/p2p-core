package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.core.application.EchoApplication;

public class ApplicationActivatorBaseTest {
    private static final Long START_WAIT_TIMEOUT = 1000L;
    private ApplicationActivatorBase applicationActivator;
    private ScheduledExecutorService scheduledExecutor;
    private ThreadPoolTaskExecutor executor;
    private ApplicationRegistry applicationRegistry;
    private EchoApplication echoApp;
    private Semaphore activationChecksCompletedSemaphore;
    private Semaphore checkAndActivateSemaphore;
    private CountDownLatch rollbackActivateLatch;
    private ApplicationActivationCheckStatus localActivationPreconditionsMet;
    private Runnable checkAndActivateRunnable;
    private Semaphore timestampInvocationSemaphore;
    private InterApplicationDependenciesStore interApplicationDependenciesStore;

    @Before
    public void before() {
        interApplicationDependenciesStore = mock(InterApplicationDependenciesStore.class);
        // signalling
        activationChecksCompletedSemaphore = new Semaphore(0);
        checkAndActivateSemaphore = new Semaphore(0);
        timestampInvocationSemaphore = new Semaphore(0);
        rollbackActivateLatch = new CountDownLatch(1);
        localActivationPreconditionsMet = ApplicationActivationCheckStatus.ACTIVATE;

        // deps
        applicationRegistry = new ApplicationRegistry();
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

        applicationActivator = new ApplicationActivatorBase() {
            @Override
            protected ApplicationActivationCheckStatus checkLocalActivationPreconditions(ActivationAwareApplication application) {
                return localActivationPreconditionsMet;
            }

            @Override
            public ApplicationActivationCheckStatus initiateActivationChecks(ActivationAwareApplication application) {
                ApplicationActivationCheckStatus result = super.initiateActivationChecks(application);
                activationChecksCompletedSemaphore.release();
                return result;
            }

            @Override
            protected void rollbackApplicationActivation(ActivationAwareApplication application) {
                super.rollbackApplicationActivation(application);
                rollbackActivateLatch.countDown();
            }

            @Override
            public void deActivateNode(String id, ActivationAwareApplication anActivationAwareApplication) {
            }

            @Override
            protected void checkActiveApplicationStillActiveAndHeartbeat(ActivationAwareApplication application) {
                timestampInvocationSemaphore.release();
            }

            @Override
            protected void checkAndActivate(ActivationAwareApplication application, TimerTask timerTask) {
                if (checkAndActivateRunnable != null)
                    checkAndActivateRunnable.run();
                checkAndActivateSemaphore.release();

            }
        };
        applicationActivator.setScheduledExecutorService(scheduledExecutor);
        applicationActivator.setExecutor(executor);
        applicationActivator.setApplicationRegistry(applicationRegistry);
        ReflectionTestUtils.setField(applicationActivator, "interApplicationDependenciesStore", interApplicationDependenciesStore);
    }

    @Test
    public void shouldInitiateActivationOnRegister() throws Exception {
        // act
        applicationActivator.register(echoApp);

        // assert
        assertTrue(activationChecksCompletedSemaphore.tryAcquire(5, TimeUnit.SECONDS));
        assertTrue(checkAndActivateSemaphore.tryAcquire(5, TimeUnit.SECONDS));
        assertEquals(ApplicationStatus.CHECKING, applicationRegistry.getApplicationStatus(echoApp.getApplicationName()));
    }

    @Test
    public void shouldNotStartButShouldTimestampIfAlreadyActive() throws Exception {
        // setup
        checkAndActivateRunnable = new Runnable() {
            @Override
            public void run() {
                applicationRegistry.setApplicationStatus(echoApp.getApplicationName(), ApplicationStatus.ACTIVE);
            }
        };
        applicationActivator.register(echoApp);
        checkAndActivateSemaphore.tryAcquire(5, TimeUnit.SECONDS);

        // act
        applicationActivator.initiateActivationChecks(echoApp);

        // assert
        assertTrue(activationChecksCompletedSemaphore.tryAcquire(2, 5, TimeUnit.SECONDS));
        assertFalse(checkAndActivateSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS));
        assertTrue(timestampInvocationSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
    }

    @Test
    public void shouldGoToPassiveWhenLocalPreconditionsNotMet() throws Exception {
        // setup
        localActivationPreconditionsMet = ApplicationActivationCheckStatus.PASSIFY;

        // act
        applicationActivator.register(echoApp);

        // assert
        assertTrue(activationChecksCompletedSemaphore.tryAcquire(5, TimeUnit.SECONDS));
        assertEquals(ApplicationStatus.PASSIVE, applicationRegistry.getApplicationStatus(echoApp.getApplicationName()));
        verify(echoApp, never()).becomeActive();
    }

    @Test
    public void shouldCancelActivationWhenItTakesTooLong() throws Exception {
        // setup
        checkAndActivateRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        };

        // act
        applicationActivator.register(echoApp);

        // assert
        assertTrue(rollbackActivateLatch.await(5, TimeUnit.SECONDS));
        assertFalse(checkAndActivateSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS));
        assertEquals(ApplicationStatus.PASSIVE, applicationRegistry.getApplicationStatus(echoApp.getApplicationName()));
        verify(echoApp).becomePassive();
    }

    @Test
    public void shouldRunMultipleTimesAtSetIntervals() throws Exception {
        // setup
        when(echoApp.getActivationCheckPeriodSecs()).thenReturn(1);

        // act
        applicationActivator.register(echoApp);

        // assert
        assertTrue(activationChecksCompletedSemaphore.tryAcquire(3, 5, TimeUnit.SECONDS));
    }
}

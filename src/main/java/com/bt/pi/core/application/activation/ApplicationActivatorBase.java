package com.bt.pi.core.application.activation;

import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.core.conf.Property;

public abstract class ApplicationActivatorBase implements ApplicationActivator {
    protected static final String DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS = "10";
    private static final Log LOG = LogFactory.getLog(ApplicationActivatorBase.class);
    private static final String DEFAULT_MIN_VALUE_FOR_RETRY_INTERVAL_SECONDS = "3";
    private static final Random RANDOMIZER = new Random();
    private ApplicationRegistry applicationRegistry;
    private ScheduledExecutorService scheduledExecutorService;
    private Executor executor;
    private int minValueForRetryIntervalSeconds;
    private int maxValueForRandomIntervalOffsetSeconds;
    private InterApplicationDependenciesStore interApplicationDependenciesStore;

    public ApplicationActivatorBase() {
        applicationRegistry = null;
        executor = null;
        scheduledExecutorService = null;
        minValueForRetryIntervalSeconds = Integer.parseInt(DEFAULT_MIN_VALUE_FOR_RETRY_INTERVAL_SECONDS);
        maxValueForRandomIntervalOffsetSeconds = Integer.parseInt(DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS);
        interApplicationDependenciesStore = null;
    }

    @Property(key = "activation.min.retry.interval.", defaultValue = DEFAULT_MIN_VALUE_FOR_RETRY_INTERVAL_SECONDS)
    public void setMinValueForRetryIntervalSeconds(int aMinValueForRetryIntervalSeconds) {
        this.minValueForRetryIntervalSeconds = aMinValueForRetryIntervalSeconds;
    }

    @Property(key = "activation.max.random.interval.offset", defaultValue = DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS)
    public void setMaxValueForRandomIntervalOffsetSeconds(int maxValueForRandomIntervalOffset) {
        this.maxValueForRandomIntervalOffsetSeconds = maxValueForRandomIntervalOffset;
    }

    @Resource
    public void setApplicationRegistry(ApplicationRegistry anApplicationRegistry) {
        this.applicationRegistry = anApplicationRegistry;
    }

    public ApplicationRegistry getApplicationRegistry() {
        return applicationRegistry;
    }

    @Override
    public ApplicationStatus getApplicationStatus(String appName) {
        return applicationRegistry.getApplicationStatus(appName);
    }

    @Resource(type = ThreadPoolTaskExecutor.class)
    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        this.scheduledExecutorService = aScheduledExecutorService;
    }

    protected Executor getExecutor() {
        return executor;
    }

    @Resource
    public void setInterApplicationDependenciesStore(InterApplicationDependenciesStore aInterApplicationDependenciesStore) {
        interApplicationDependenciesStore = aInterApplicationDependenciesStore;
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    protected int getSizeOfPreferablyExcludedApplicationsForApplication(ActivationAwareApplication anActivationAwareApplication) {
        String applicationName = anActivationAwareApplication.getApplicationName();
        int size = getNumberOfApplicationInstancesFor(applicationName) - 1;

        List<String> preferablyExcludedApplications = interApplicationDependenciesStore.getPreferablyExcludedApplications(applicationName);
        if (preferablyExcludedApplications != null) {
            for (String preferablyExcludedApplication : preferablyExcludedApplications) {
                size += getNumberOfApplicationInstancesFor(preferablyExcludedApplication);
            }
        }

        return size;
    }

    private int getNumberOfApplicationInstancesFor(String applicationName) {
        LOG.debug(String.format("getNumberOfApplicationInstancesFor(%s)", applicationName));
        try {
            ApplicationRecord cachedApplicationRecord = applicationRegistry.getCachedApplicationRecord(applicationName);
            return cachedApplicationRecord == null ? 1 : cachedApplicationRecord.getRequiredActive();
        } catch (UnknownApplicationException e) {
            LOG.warn(String.format("Cannot get application record for %s, treating it as 0 instances", applicationName));
            return 1;
        }
    }

    @Override
    public void register(final ActivationAwareApplication anActivationAwareApplication) {
        getApplicationRegistry().registerApplication(anActivationAwareApplication);

        getScheduledExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                ApplicationActivationCheckStatus result = ApplicationActivationCheckStatus.ACTIVATE;
                try {
                    result = initiateActivationChecks(anActivationAwareApplication);
                    LOG.debug(String.format("Activation checks for %s returned %s", anActivationAwareApplication.getApplicationName(), result));
                } catch (Throwable t) {
                    LOG.error(String.format("Exception while trying to kick off app activator: %s", t.getMessage()), t);
                } finally {
                    int scheduleInterval = anActivationAwareApplication.getActivationCheckPeriodSecs();
                    if (ApplicationActivationCheckStatus.RETRY == result)
                        scheduleInterval = minValueForRetryIntervalSeconds + RANDOMIZER.nextInt(maxValueForRandomIntervalOffsetSeconds);

                    LOG.debug(String.format("Application %s will retry activation check in %d seconds", anActivationAwareApplication.getApplicationName(), scheduleInterval));
                    getScheduledExecutorService().schedule(this, scheduleInterval, TimeUnit.SECONDS);
                }
            }
        });
    }

    protected void executeApplicationActivation(final ActivationAwareApplication application) {
        LOG.debug(String.format("executeApplicationActivation()"));
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean res = false;
                    try {
                        res = application.becomeActive();
                        LOG.debug(String.format("application %s.becomeActive() returned %s", application.getApplicationName(), res));
                    } catch (RuntimeException e) {
                        LOG.error(e.getMessage(), e);
                    }

                    if (res) {
                        // TODO: do the following app state check as an atomic state machine update
                        ApplicationStatus currentStatus = getApplicationRegistry().getApplicationStatus(application.getApplicationName());
                        LOG.debug("current status: " + currentStatus);
                        if (currentStatus == ApplicationStatus.CHECKING) {
                            getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.ACTIVE);
                        } else {
                            LOG.warn(String.format("App state for app %s was unexpectedly %s instead of %s - not going to set to ACTIVE", application.getApplicationName(), currentStatus, ApplicationStatus.CHECKING));
                        }
                    } else {
                        rollbackApplicationActivation(application);
                    }
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
            }
        });
    }

    public ApplicationActivationCheckStatus initiateActivationChecks(final ActivationAwareApplication application) {
        LOG.debug(String.format("Initiate activation checks(%s)", application.getApplicationName()));
        ApplicationStatus appStatus = getApplicationRegistry().getApplicationStatus(application.getApplicationName());
        if (appStatus.equals(ApplicationStatus.ACTIVE)) {
            LOG.debug(String.format("Application %s is currently active, going to timestamp it", application.getApplicationName()));
            checkActiveApplicationStillActiveAndHeartbeat(application);
            return ApplicationActivationCheckStatus.PASSIFY;
        }

        if (getApplicationRegistry().getApplicationStatus(application.getApplicationName()) == ApplicationStatus.CHECKING) {
            LOG.warn(String.format("Application status is CHECKING just before a new check is initiated"));
        }

        getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.CHECKING);
        ApplicationActivationCheckStatus localActivationPreconditions = checkLocalActivationPreconditions(application);
        if (ApplicationActivationCheckStatus.ACTIVATE != localActivationPreconditions) {
            LOG.info(String.format("Precondition checks say app %s should not activate", application.getApplicationName()));
            getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
            return localActivationPreconditions;
        }

        // Timer to clean up if app fails to start
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                LOG.debug(String.format("Checking whether or not app start for %s succeeded", application.getApplicationName()));
                try {
                    ApplicationStatus appStatus = getApplicationRegistry().getApplicationStatus(application.getApplicationName());
                    if (appStatus == ApplicationStatus.CHECKING) {
                        LOG.warn(String.format("Application %s failed to start in a timely manner - setting it to passive", application.getApplicationName()));
                        rollbackApplicationActivation(application);
                    }
                } catch (Throwable t) {
                    LOG.error(t.getMessage(), t);
                }
            }
        };
        getScheduledExecutorService().schedule(timerTask, application.getStartTimeout(), application.getStartTimeoutUnit());

        checkAndActivate(application, timerTask);

        return localActivationPreconditions;
    }

    /**
     * This is a BLOCKING call and hence should be local. Its purpose is to verify local node state before commencing
     * activation. This might be stuff like is this app already running, or are there other apps running that would
     * block this one from activating.
     * 
     * @return true if app can attempt activation
     */
    protected abstract ApplicationActivationCheckStatus checkLocalActivationPreconditions(ActivationAwareApplication application);

    /**
     * Perform any further activation checks specific to the implementation, such as checking shared state to see if
     * thsi app should go active - then tell it to do so. The implementation of this method is responsible for setting
     * application state from checking to active or passive.
     */
    protected abstract void checkAndActivate(ActivationAwareApplication application, TimerTask timerTask);

    /**
     * Called to timestamp active application
     */
    protected abstract void checkActiveApplicationStillActiveAndHeartbeat(ActivationAwareApplication application);

    /**
     * This method is called when an activation attempt has failed. It needs to reset all state for the app to passive,
     * and tell the app to go passive.
     */
    protected void rollbackApplicationActivation(ActivationAwareApplication application) {
        // set this application to passive
        getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
        // call stop on the application just incase
        application.becomePassive();
    }
}

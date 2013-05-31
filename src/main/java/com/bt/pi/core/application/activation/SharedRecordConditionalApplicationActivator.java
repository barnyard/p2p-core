package com.bt.pi.core.application.activation;

import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

/**
 * ApplicationActivator base class that checks an ApplicationRecord entity in the DTH to coordinate activation of
 * applications. The ApplicationRecord url is derived from the Application name.
 * 
 */
public abstract class SharedRecordConditionalApplicationActivator extends ApplicationActivatorBase implements ScopedApplicationActivator, ApplicationContextAware {
    protected static final String DEFAULT_ACTIVATION_EXPIRY_MULTIPLICATION_FACTOR = "1.5";
    private static final String CANCELING_CLEANUP_TASK_SUCCESSFUL = "Canceling cleanup task successful: ";
    private static final String RETURNING_NULL = "returning null";
    private static final String PROVISIONALLY_READ_APP_RECORD_S_GOING_TO_CACHE_IT = "Provisionally read app record %s - going to cache it";
    private static final String OH_DEAR_MISSING_APP_RECORD_IN_DHT_FOR_APP_S = "Oh dear - missing app record in DHT for app %s!!";
    private static final Log LOG = LogFactory.getLog(SharedRecordConditionalApplicationActivator.class);
    private KoalaIdFactory koalaIdFactory;
    private DhtClientFactory dhtClientFactory;
    private ApplicationContext applicationContext;
    private int maxValueForRandomIntervalOffsetSeconds;
    private double activationExpiryMultiplicationFactor;

    public SharedRecordConditionalApplicationActivator() {
        koalaIdFactory = null;
        dhtClientFactory = null;
        applicationContext = null;
        maxValueForRandomIntervalOffsetSeconds = Integer.parseInt(ApplicationActivatorBase.DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS);
        activationExpiryMultiplicationFactor = Double.parseDouble(DEFAULT_ACTIVATION_EXPIRY_MULTIPLICATION_FACTOR);
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource(type = KoalaIdFactory.class)
    public void setKoalaIdFactory(KoalaIdFactory aIdFactory) {
        LOG.debug(String.format("setIdFactory(%s)", aIdFactory));
        this.koalaIdFactory = aIdFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext anApplicationContext) {
        this.applicationContext = anApplicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }

    protected PId getApplicationRecordId(String appName, NodeScope scope) {
        String applicationUrl = null;

        if (scope.equals(NodeScope.AVAILABILITY_ZONE)) {
            applicationUrl = AvailabilityZoneScopedApplicationRecord.getUrl(appName);
        } else if (scope.equals(NodeScope.REGION)) {
            applicationUrl = RegionScopedApplicationRecord.getUrl(appName);
        } else {
            applicationUrl = GlobalScopedApplicationRecord.getUrl(appName);
        }

        return koalaIdFactory.buildPId(applicationUrl).forLocalScope(scope);
    }

    @Override
    protected ApplicationActivationCheckStatus checkLocalActivationPreconditions(final ActivationAwareApplication application) {
        ApplicationActivationCheckStatus result = checkInterApplicationDependencies(application);
        if (ApplicationActivationCheckStatus.PASSIFY.equals(result)) {
            // update the cached app record in the registry even if the app is passive
            PId appRecordId = getApplicationRecordId(application.getApplicationName(), getActivationScope());
            getDhtClientFactory().createReader().getAsync(appRecordId, new PiContinuation<ApplicationRecord>() {
                @Override
                public void handleResult(ApplicationRecord result) {
                    getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), result);
                }
            });
        }
        return result;
    }

    @Override
    public void checkAndActivate(ActivationAwareApplication application, TimerTask cleanupTask) {
        checkSharedRecordAndActivate(application, cleanupTask);
    }

    @Override
    protected void rollbackApplicationActivation(ActivationAwareApplication application) {
        LOG.debug(String.format("rollbackApplicationActivation(%s)", application.getApplicationName()));
        // update application record
        makeAppInactiveInSharedRecord(application);
        super.rollbackApplicationActivation(application);
    }

    private ApplicationActivationCheckStatus checkInterApplicationDependencies(final ActivationAwareApplication application) {
        LOG.debug(String.format("checkInterApplicationDependencies for %s", application.getApplicationName()));

        int preferablyExcludedApplicationsSize = getSizeOfPreferablyExcludedApplicationsForApplication(application);
        int leafsetSize = getLeafsetSizeForAvailabilityZone(application);

        LOG.debug(String.format("Application %s has %d entries in its preferably excluded applications and its leafset size is %d", application.getApplicationName(), preferablyExcludedApplicationsSize, leafsetSize));
        if (preferablyExcludedApplicationsSize > leafsetSize) {
            LOG.warn(String.format("Leafset is too small to honour preferably excluded applications list for application %s, so not checking interapplication dependencies", application.getApplicationName()));
            return ApplicationActivationCheckStatus.ACTIVATE;
        }

        ApplicationActivationCheckStatus activationPreconditionsMet = ApplicationActivationCheckStatus.ACTIVATE;
        List<String> preferablyExcludedApplications = application.getPreferablyExcludedApplications();
        for (int i = 0; preferablyExcludedApplications != null && i < preferablyExcludedApplications.size(); i++) {
            ApplicationStatus preferablyExcludedAppStatus;
            try {
                preferablyExcludedAppStatus = getApplicationRegistry().getApplicationStatus(preferablyExcludedApplications.get(i));
                LOG.debug(String.format("Application %s has status %s", preferablyExcludedApplications.get(i), preferablyExcludedAppStatus));
            } catch (UnknownApplicationException e) {
                LOG.debug(String.format("Ignoring app %s for the purpose of checking inter app deps for %s, as that app doesn't yet exist", application.getPreferablyExcludedApplications().get(i), application.getApplicationName()));
                continue;
            }
            if (preferablyExcludedAppStatus.equals(ApplicationStatus.ACTIVE)) {
                LOG.info(String.format("Application %s cannot start as app %s is active", application.getApplicationName(), application.getPreferablyExcludedApplications().get(i)));
                activationPreconditionsMet = ApplicationActivationCheckStatus.PASSIFY;
                break;
            }

            if (preferablyExcludedAppStatus.equals(ApplicationStatus.CHECKING)) {
                activationPreconditionsMet = ApplicationActivationCheckStatus.RETRY;
                break;
            }
        }
        LOG.debug(String.format("checkInterApplicationDependencies() for %s returning %s", application.getApplicationName(), activationPreconditionsMet));
        return activationPreconditionsMet;
    }

    private int getLeafsetSizeForAvailabilityZone(final ActivationAwareApplication application) {
        Collection<NodeHandle> leafNodeHandles = application.getLeafNodeHandles();
        int leafsetSize = leafNodeHandles.size();

        int applicationAvzCode = koalaIdFactory.getAvailabilityZoneWithinRegion();
        int applicationRegionCode = koalaIdFactory.getRegion();
        for (NodeHandle nodeHandle : leafNodeHandles) {
            int regionCode = PId.getRegionFromId(nodeHandle.getNodeId().toStringFull());
            int avzCode = PId.getAvailabilityZoneFromId(nodeHandle.getNodeId().toStringFull());
            if (regionCode != applicationRegionCode || avzCode != applicationAvzCode)
                leafsetSize--;

        }
        return leafsetSize;
    }

    protected void checkSharedRecordAndActivate(final ActivationAwareApplication application, final TimerTask cleanupTask) {
        LOG.debug(String.format("Going to check application record for app %s", application.getApplicationName()));
        PId appRecordId = getApplicationRecordId(application.getApplicationName(), getActivationScope());
        LOG.debug(String.format("Checking with id %s", appRecordId.toStringFull()));
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(appRecordId, null, new UpdateResolvingPiContinuation<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                if (existingEntity == null) {
                    LOG.warn(String.format(OH_DEAR_MISSING_APP_RECORD_IN_DHT_FOR_APP_S, application.getApplicationName()));
                    return null;
                }

                LOG.debug(String.format(PROVISIONALLY_READ_APP_RECORD_S_GOING_TO_CACHE_IT, existingEntity));
                getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), existingEntity);
                if (existingEntity.addCurrentlyActiveNode(application.getNodeId(), getActivationTimestampExpiry(application))) {
                    LOG.debug("Adding node " + application.getNodeId() + " to active list for application " + application.getApplicationName());
                    return existingEntity;
                } else {
                    LOG.debug(RETURNING_NULL);
                    return null;
                }
            }

            @Override
            public void handleException(Exception e) {
                LOG.error(String.format("Exception when trying to activate application %s: %s", application.getApplicationName(), e.getMessage()), e);
                getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
                if (cleanupTask != null) {
                    LOG.debug(CANCELING_CLEANUP_TASK_SUCCESSFUL + cleanupTask.cancel());
                }
            }

            @Override
            public void handleResult(ApplicationRecord valueWritten) {
                LOG.debug(String.format("Received result: %s", valueWritten));
                if (valueWritten != null) {
                    getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), (ApplicationRecord) valueWritten);
                    executeApplicationActivation(application);
                } else {
                    LOG.debug(String.format("App record written was null - app going passive"));
                    getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
                }
                if (cleanupTask != null) {
                    LOG.debug(CANCELING_CLEANUP_TASK_SUCCESSFUL + cleanupTask.cancel());
                }
                publishRefreshedAppRecordEvent(application);
            }
        });
    }

    @Property(key = "activation.max.random.interval.offset", defaultValue = ApplicationActivatorBase.DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS)
    public void setMaxValueForRandomIntervalOffsetSeconds(int maxValueForRandomIntervalOffset) {
        this.maxValueForRandomIntervalOffsetSeconds = maxValueForRandomIntervalOffset;
    }

    @Property(key = "activation.expiry.multiplication.factor", defaultValue = DEFAULT_ACTIVATION_EXPIRY_MULTIPLICATION_FACTOR)
    public void setActivationExpiryMultiplicationFactor(double value) {
        this.activationExpiryMultiplicationFactor = value;
    }

    private long getActivationTimestampExpiry(ActivationAwareApplication application) {
        LOG.debug(String.format("getActivationTimestampExpiry(%s)", application.getApplicationName()));
        long result = (long) ((application.getActivationCheckPeriodSecs() + this.maxValueForRandomIntervalOffsetSeconds) * this.activationExpiryMultiplicationFactor);
        return result;
    }

    private void publishRefreshedAppRecordEvent(final ActivationAwareApplication application) {
        LOG.debug(String.format("publishRefreshedAppRecordEvent(%s)", application));
        if (application == null) {
            LOG.warn(String.format("Null application - not publishing event"));
            return;
        }

        String applicationName = application.getApplicationName();
        ApplicationRecord cachedApplicationRecord = getApplicationRegistry().getCachedApplicationRecord(applicationName);
        if (cachedApplicationRecord != null) {
            ApplicationRecordRefreshedEvent event = new ApplicationRecordRefreshedEvent(cachedApplicationRecord, this);
            applicationContext.publishEvent(event);
        } else {
            LOG.debug(String.format("Did not publish refreshed app rec event for app %s, as no cached record was found", applicationName));
        }
    }

    protected void makeAppInactiveInSharedRecord(final ActivationAwareApplication application) {
        LOG.debug(String.format("Going to make app %s inactive in shared app record", application.getApplicationName()));
        PId appRecordId = getApplicationRecordId(application.getApplicationName(), getActivationScope());
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(appRecordId, null, new UpdateResolvingPiContinuation<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                if (existingEntity == null) {
                    LOG.warn(String.format(OH_DEAR_MISSING_APP_RECORD_IN_DHT_FOR_APP_S, application.getApplicationName()));
                    return null;
                }

                LOG.debug(String.format(PROVISIONALLY_READ_APP_RECORD_S_GOING_TO_CACHE_IT, existingEntity));
                getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), existingEntity);
                if (existingEntity.removeActiveNode(application.getNodeId())) {
                    LOG.debug("Removing node " + application.getNodeId() + " from active list for application " + application.getApplicationName());
                    return existingEntity;
                } else {
                    LOG.debug(RETURNING_NULL);
                    return null;
                }
            }

            @Override
            public void handleException(Exception e) {
                LOG.error(String.format("Exception when trying to make application %s inactive: %s", application.getApplicationName(), e.getMessage()), e);
            }

            @Override
            public void handleResult(ApplicationRecord valueWritten) {
                if (valueWritten != null) {
                    getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), (ApplicationRecord) valueWritten);
                    publishRefreshedAppRecordEvent(application);
                } else {
                    LOG.debug(String.format("App record value written for app %s was null - app was not active", application.getApplicationName()));
                }

            }
        });
    }

    @Override
    protected void checkActiveApplicationStillActiveAndHeartbeat(final ActivationAwareApplication application) {
        LOG.debug(String.format("heartbeatActiveApplication(%s)", application.getApplicationName()));
        PId appRecordId = getApplicationRecordId(application.getApplicationName(), getActivationScope());
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(appRecordId, null, new UpdateResolvingPiContinuation<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                LOG.debug(String.format(PROVISIONALLY_READ_APP_RECORD_S_GOING_TO_CACHE_IT, existingEntity));
                getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), existingEntity);

                boolean timestamped = existingEntity.timestampActiveNode(application.getNodeId());
                if (timestamped) {
                    return existingEntity;
                } else {
                    LOG.info(String.format("Did not find %s as active node to timestamp in app record for %s. This will initiate application passivation.", application.getNodeId(), application.getApplicationName()));
                    return null;
                }
            }

            @Override
            public void handleException(Exception exception) {
                LOG.error(String.format("Exception when trying to heartbeat active application %s", application.getApplicationName()), exception);
            }

            @Override
            public void handleResult(ApplicationRecord result) {
                LOG.debug(String.format("Heartbeat for active application %s wrote app record %s", application.getApplicationName(), result));
                if (result != null) {
                    getApplicationRegistry().setCachedApplicationRecord(application.getApplicationName(), result);
                    publishRefreshedAppRecordEvent(application);
                } else {
                    LOG.info(String.format("As app %s was not timestamped, it will be passivated.", application.getApplicationName()));
                    rollbackApplicationActivation(application);
                }
            }
        });
    }

    public Id getClosestActiveApplicationNodeId(String applicationName, Id anId) {
        LOG.debug(String.format("getClosestActiveApplicationNodeId(%s)", anId.toStringFull()));
        ApplicationRecord applicationRecord = getApplicationRegistry().getCachedApplicationRecord(applicationName);
        if (applicationRecord == null || applicationRecord.getActiveNodeMap().isEmpty()) {
            LOG.info(String.format("Zero active apps or active app record not cached - cannot identify nearest app instance for id %s", anId));
            return null;
        }

        return applicationRecord.getClosestActiveNodeId(anId);
    }

    public void deActivateNode(final String id, final ActivationAwareApplication application) {
        PId appRecordId = getApplicationRecordId(application.getApplicationName(), getActivationScope());
        DhtWriter writer = dhtClientFactory.createWriter();
        writer.update(appRecordId, new UpdateResolvingPiContinuation<ApplicationRecord>() {
            @Override
            public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
                LOG.debug(String.format("Removing %s from %s application Record.", id, application.getApplicationName()));
                boolean b = existingEntity.removeActiveNode(id);
                LOG.debug("Remove successful: " + b);
                return existingEntity;
            }

            @Override
            public void handleResult(ApplicationRecord result) {
                LOG.debug(String.format("Result of deactivating node %s is: %s", id, result));
                initiateActivationChecks(application);
            }
        });
    }
}

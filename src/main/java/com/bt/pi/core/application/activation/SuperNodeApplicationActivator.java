package com.bt.pi.core.application.activation;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

/**
 * An application activator that uses proximity to a supernode checkpoint to coordinate node activation. Only one node
 * to a particular checkpoint will be activated. If a single node is the closest to two checkpoints there will be one
 * less supernode in the ring. The number of supernodes per application is designated in the
 * SuperNodeApplicationCheckPoints entity seeded in the DHT.
 * 
 */
@Component
public class SuperNodeApplicationActivator extends ApplicationActivatorBase {
    private static final String CANCELING_LONG_STARTUP_ROLLBACK_TASK_SUCCESSFULLY_S = "Canceling long startup rollback task. Successfully: %s";

    private static final Log LOG = LogFactory.getLog(SuperNodeApplicationActivator.class);

    private KoalaIdFactory koalaIdFactory;
    private DhtCache dhtCache;
    private KoalaIdUtils koalaIdUtils;

    public SuperNodeApplicationActivator() {
        koalaIdFactory = null;
        dhtCache = null;
        koalaIdUtils = null;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        koalaIdFactory = aKoalaIdFactory;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        dhtCache = aDhtCache;
    }

    @Resource
    public void setKoalaIdUtils(KoalaIdUtils aKoalaIdUtils) {
        koalaIdUtils = aKoalaIdUtils;
    }

    @Override
    protected void checkActiveApplicationStillActiveAndHeartbeat(final ActivationAwareApplication application) {
        goActiveOrPassive(application, null);
    }

    @Override
    protected void checkAndActivate(final ActivationAwareApplication application, TimerTask cleanupTask) {
        goActiveOrPassive(application, cleanupTask);
    }

    private void goActiveOrPassive(final ActivationAwareApplication application, final TimerTask cleanupTask) {
        PId id = koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL);
        dhtCache.get(id, new PiContinuation<SuperNodeApplicationCheckPoints>() {
            @Override
            public void handleException(Exception e) {
                if (cleanupTask != null) {
                    LOG.debug(String.format(CANCELING_LONG_STARTUP_ROLLBACK_TASK_SUCCESSFULLY_S, cleanupTask.cancel()));
                }
            };

            @Override
            public void handleResult(SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints) {
                Set<String> superNodeCheckPoints = null;
                if (superNodeApplicationCheckPoints == null)
                    LOG.info("No super nodes specified in the dht");
                else {
                    superNodeCheckPoints = superNodeApplicationCheckPoints.getSuperNodeCheckPoints(application.getApplicationName(), koalaIdFactory.getRegion(), koalaIdFactory.getAvailabilityZoneWithinRegion());

                    for (String superNodeCheckPoint : superNodeCheckPoints) {
                        if (isClosestNodeToSuperNodeCheckPoint(application, superNodeCheckPoint)) {
                            executeApplicationActivation(application);
                            return;
                        }
                    }
                }

                application.becomePassive();
                getApplicationRegistry().setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
                if (cleanupTask != null) {
                    LOG.debug(String.format(CANCELING_LONG_STARTUP_ROLLBACK_TASK_SUCCESSFULLY_S, cleanupTask.cancel()));
                }
            }
        });
    }

    private boolean isClosestNodeToSuperNodeCheckPoint(ActivationAwareApplication application, String superNodeCheckPoint) {
        Id localNodeId = koalaIdFactory.generateNodeId();

        SortedSet<String> nodeIds = new TreeSet<String>();
        nodeIds.add(localNodeId.toStringFull());
        Collection<NodeHandle> leafNodeHandles = application.getLeafNodeHandles();
        for (NodeHandle nodeHandle : leafNodeHandles) {
            String remoteNodeId = nodeHandle.getNodeId().toStringFull();
            if (PId.getRegionFromId(remoteNodeId) == koalaIdFactory.getRegion() && PId.getAvailabilityZoneFromId(remoteNodeId) == koalaIdFactory.getAvailabilityZoneWithinRegion())
                nodeIds.add(remoteNodeId);
        }

        Id nodeIdClosestToId = koalaIdUtils.getNodeIdClosestToId(nodeIds, koalaIdFactory.buildId(superNodeCheckPoint), NodeScope.AVAILABILITY_ZONE);
        LOG.debug(String.format("%s is closest to %s (local id is %s)", superNodeCheckPoint, nodeIdClosestToId.toStringFull(), localNodeId.toStringFull()));
        return localNodeId.equals(nodeIdClosestToId);
    }

    @Override
    protected ApplicationActivationCheckStatus checkLocalActivationPreconditions(ActivationAwareApplication application) {
        LOG.debug(String.format("checkLocalActivationPreconditions(%s)", application.getApplicationName()));
        return ApplicationActivationCheckStatus.ACTIVATE;
    }

    @Override
    public void deActivateNode(String id, ActivationAwareApplication anActivationAwareApplication) {
    }
}

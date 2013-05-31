package com.bt.pi.core.application.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;

import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class DefaultDhtResourceRefreshRunner<T extends PiEntity> implements Runnable {
    private static final Log LOG = LogFactory.getLog(DefaultDhtResourceRefreshRunner.class);
    private CachingConsumedResourceRegistry sharedResourceManager;
    private PId resourceId;
    private Continuation<T, Exception> refreshHandlingContinuation;
    private LeasedResourceAllocationRecordHeartbeater leasedResourceAllocationRecordHeartbeater;

    public DefaultDhtResourceRefreshRunner(PId aResourceId, CachingConsumedResourceRegistry aSharedResourceManager, LeasedResourceAllocationRecordHeartbeater aLeasedResourceAllocationRecordHeartbeater) {
        this(aResourceId, aSharedResourceManager, aLeasedResourceAllocationRecordHeartbeater, new LoggingContinuation<T>());
    }

    public DefaultDhtResourceRefreshRunner(PId aResourceId, CachingConsumedResourceRegistry aSharedResourceManager, LeasedResourceAllocationRecordHeartbeater aLeasedResourceAllocationRecordHeartbeater,
            Continuation<T, Exception> aRefreshHandlingContinuation) {
        LOG.debug(String.format("constructor(%s, %s, %s, %s)", aSharedResourceManager, aResourceId, aLeasedResourceAllocationRecordHeartbeater, aRefreshHandlingContinuation));
        sharedResourceManager = aSharedResourceManager;
        resourceId = aResourceId;
        leasedResourceAllocationRecordHeartbeater = aLeasedResourceAllocationRecordHeartbeater;
        refreshHandlingContinuation = aRefreshHandlingContinuation;
    }

    public CachingConsumedResourceRegistry getSharedResourceManager() {
        return sharedResourceManager;
    }

    public PId getResourceId() {
        return resourceId;
    }

    public Continuation<T, Exception> getRefresHandlingContinuation() {
        return refreshHandlingContinuation;
    }

    @Override
    public void run() {
        LOG.debug(String.format("run() for resource %s, continuation %s", resourceId, refreshHandlingContinuation));
        sharedResourceManager.refresh(resourceId, new GenericContinuation<T>() {
            @Override
            public void handleResult(T result) {
                leasedResourceAllocationRecordHeartbeater.timestampLeasedAllocatedResources(result);
                refreshHandlingContinuation.receiveResult(result);
            }

            @Override
            public void handleException(Exception exception) {
                refreshHandlingContinuation.receiveException(exception);
            }
        });
    }
}

package com.bt.pi.core.application.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@Component
@Scope("prototype")
public final class DefaultDhtResourceWatchingStrategy extends AbstractResourceWatchingStrategy<PId, PiEntity> {
    private static final Log LOG = LogFactory.getLog(DefaultDhtResourceWatchingStrategy.class);

    public DefaultDhtResourceWatchingStrategy() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Runnable getSharedResourceRefreshRunner(PId resourceId) {
        LOG.debug(String.format("getSharedResourceRefreshRunner(%s)", resourceId));
        return new DefaultDhtResourceRefreshRunner(resourceId, getCachingConsumedResourceRegistry(), getLeasedResourceAllocationRecordHeartbeater());
    }
}

/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.reporter.NodeBasedReportableEntityStore;
import com.bt.pi.core.application.reporter.ReportHandler;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.PiEntityCollection;

@Component
public class NodePhysicalHealthHandler extends ReportHandler<HeartbeatEntity> {
    private static final Log LOG = LogFactory.getLog(NodePhysicalHealthHandler.class);

    public NodePhysicalHealthHandler() {

        super(Arrays.asList(new String[] { HeartbeatEntity.class.getSimpleName(), new HeartbeatEntityCollection().getType() }));
        setReportableEntityStore(new NodeBasedReportableEntityStore<HeartbeatEntity>());
    }

    @Property(key = "health.broadcastwindowsize", defaultValue = DEFAULT_BROADCAST_WINDOW)
    @Override
    public void setBroadcastWindowSize(int windowSize) {
        super.setBroadcastWindowSize(windowSize);
    }

    @Property(key = "health.publishintervalsize", defaultValue = DEFAULT_PUBLISH_DELAY_SECONDS)
    @Override
    public void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        super.setPublishIntervalSeconds(aPublishIntervalSeconds);
    }

    public HeartbeatEntity getEntityByNodeId(String nodeId) {
        return ((NodeBasedReportableEntityStore<HeartbeatEntity>) getReportableEntityStore()).getByNodeId(nodeId);
    }

    @Override
    protected PiEntityCollection<HeartbeatEntity> getPiEntityCollection() {
        return new HeartbeatEntityCollection();
    }

    public void receive(HeartbeatEntity heartbeatEntity) {
        LOG.debug(String.format("receive(%s)", heartbeatEntity));
        ((NodeBasedReportableEntityStore<HeartbeatEntity>) getReportableEntityStore()).add(heartbeatEntity);
    }
}

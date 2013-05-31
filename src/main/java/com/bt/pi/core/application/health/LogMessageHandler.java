package com.bt.pi.core.application.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.application.reporter.ReportHandler;
import com.bt.pi.core.application.reporter.SizeBoundReportableEntityStore;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.util.collections.ConcurrentSortedBoundQueue;

@Component
public class LogMessageHandler extends ReportHandler<LogMessageEntity> {
    protected static final String DEFAULT_KEEPCOUNT = "250";

    public LogMessageHandler() {
        super(Arrays.asList(new String[] { new LogMessageEntityCollection().getType() }));
    }

    @Property(key = "logmessages.inmemorycount", defaultValue = DEFAULT_KEEPCOUNT)
    public void setKeepCount(int value) {
        setReportableEntityStore(new SizeBoundReportableEntityStore<LogMessageEntity>(value));
    }

    @Property(key = "logmessages.broadcastwindowsize", defaultValue = DEFAULT_BROADCAST_WINDOW)
    @Override
    public void setBroadcastWindowSize(int windowSize) {
        super.setBroadcastWindowSize(windowSize);
    }

    @Property(key = "logmessages.publishintervalsize", defaultValue = DEFAULT_PUBLISH_DELAY_SECONDS)
    @Override
    public void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        super.setPublishIntervalSeconds(aPublishIntervalSeconds);
    }

    public Collection<LogMessageEntity> getEntitiesByNodeId(String nodeId) {
        return ((ConcurrentSortedBoundQueue<LogMessageEntity>) getReportableEntityStore().getAllEntities()).getCollectionByKey(0, nodeId);
    }

    @Override
    protected PiEntityCollection<LogMessageEntity> getPiEntityCollection() {
        return new LogMessageEntityCollection();
    }

    @Override
    public PiEntityCollection<LogMessageEntity> getAllEntities() {
        PiEntityCollection<LogMessageEntity> reportableEntityCollection = getPiEntityCollection();
        Iterator<LogMessageEntity> iterator = getReportableEntityStore().getPublishIterator();
        List<LogMessageEntity> list = new ArrayList<LogMessageEntity>();
        while (iterator.hasNext())
            list.add(iterator.next());
        reportableEntityCollection.setEntities(list);
        return reportableEntityCollection;
    }
}

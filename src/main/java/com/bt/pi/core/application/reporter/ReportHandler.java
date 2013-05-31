/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.reporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;

import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.node.NodeStartedEvent;

public abstract class ReportHandler<T extends ReportableEntity<?>> implements ApplicationListener<NodeStartedEvent> {
    protected static final String DEFAULT_BROADCAST_WINDOW = "100";
    protected static final String DEFAULT_KEEPCOUNT = "5000";
    protected static final String DEFAULT_PUBLISH_DELAY_SECONDS = "300";

    private static final Log LOG = LogFactory.getLog(ReportHandler.class);
    private static final String RECEIVE_S = "receive(%s)";

    private AtomicBoolean nodeStarted;
    private int broadcastWindowSize;
    private long publishIntervalSeconds;

    private Collection<String> reportableEntityTypesHandled;
    private ReportableEntityStore<T> reportableEntityStore;
    private ReportingApplication reportingApplication;
    private ScheduledExecutorService scheduledExecutorService;

    public ReportHandler(Collection<String> theReportableEntityTypesHandled) {
        publishIntervalSeconds = Integer.parseInt(DEFAULT_PUBLISH_DELAY_SECONDS);
        reportableEntityTypesHandled = theReportableEntityTypesHandled;
        nodeStarted = new AtomicBoolean(false);
        broadcastWindowSize = Integer.parseInt(DEFAULT_BROADCAST_WINDOW);
        reportingApplication = null;
        scheduledExecutorService = null;
    }

    @PostConstruct
    public void checkReportableEntityStoreHasBeenSetAndStartScheduledThread() {
        if (reportableEntityStore == null) {
            String message = String.format("Implementing class %s has not set a reportable entity store", getClass().getSimpleName());
            LOG.error(message);
            throw new RuntimeException(message);
        }

        LOG.debug(String.format("Scheduling publish thread to run every %d seconds", publishIntervalSeconds));
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                publishUpdates();
            }
        }, 0, publishIntervalSeconds, TimeUnit.SECONDS);
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Resource
    public void setReportingApplication(ReportingApplication aReportingApplication) {
        reportingApplication = aReportingApplication;
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        nodeStarted.set(true);
    }

    protected void setBroadcastWindowSize(int windowSize) {
        broadcastWindowSize = windowSize;
    }

    protected void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        publishIntervalSeconds = aPublishIntervalSeconds;
    }

    protected void setReportableEntityStore(ReportableEntityStore<T> aReportableEntityStore) {
        reportableEntityStore = aReportableEntityStore;
    }

    protected ReportableEntityStore<T> getReportableEntityStore() {
        return reportableEntityStore;
    }

    public void receive(T data) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(RECEIVE_S, data));
        reportableEntityStore.add(data);
    }

    public void receiveFromNode(PiEntityCollection<T> data) {
        receive(data, false);
    }

    public void receive(PiEntityCollection<T> data, boolean fromSupernode) {
        LOG.debug(String.format("receive(%s,%s)", data, fromSupernode));

        if (fromSupernode)
            excludeOlderElementsWithSameId((PiEntityCollection<T>) data);
        reportableEntityStore.addAll(data.getEntities());
    }

    // if the element already exists and it's newer then we don't add it again, so therefore we remove it from the
    // entities to add
    private void excludeOlderElementsWithSameId(PiEntityCollection<T> data) {
        List<T> listToRemove = new ArrayList<T>();
        for (T entity : data.getEntities()) {
            if (reportableEntityStore.exists(entity)) {
                listToRemove.add(entity);
            }
        }
        LOG.debug("These entities already exist, not adding to cache: " + listToRemove.size());
        data.getEntities().removeAll(listToRemove);
    }

    public void publishUpdates() {
        if (nodeStarted.get()) {
            Collection<T> entities = new ArrayList<T>();
            addEntities(entities, reportableEntityStore.getPublishIterator());

            if (entities.isEmpty())
                LOG.debug("No entities in local collection, so not publishing updates to super nodes");
            else {
                PiEntityCollection<T> reportableEntityCollection = getPiEntityCollection();
                reportableEntityCollection.setEntities(entities);
                reportingApplication.publishToReportingTopic(reportableEntityCollection);
            }
        }
    }

    public PiEntityCollection<T> getAllEntities() {
        PiEntityCollection<T> reportableEntityCollection = getPiEntityCollection();
        reportableEntityCollection.setEntities(reportableEntityStore.getAllEntities());
        return reportableEntityCollection;
    }

    // Default implementation as getAllEntities.
    public PiEntityCollection<T> getEntities(PiEntityCollection piEntityCollection) {
        return getAllEntities();
    }

    public Collection<String> getReportableEntityTypesHandled() {
        return reportableEntityTypesHandled;
    }

    private void addEntities(Collection<T> entities, Iterator<T> iterator) {
        if (iterator != null) {
            for (int i = 0; i < broadcastWindowSize && iterator.hasNext(); i++)
                entities.add(iterator.next());
        }
    }

    protected abstract PiEntityCollection<T> getPiEntityCollection();
}
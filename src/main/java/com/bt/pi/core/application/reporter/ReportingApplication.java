package com.bt.pi.core.application.reporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.SuperNodeApplicationBase;
import com.bt.pi.core.application.activation.SuperNodeApplicationCheckPoints;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@Component
public class ReportingApplication extends SuperNodeApplicationBase {
    public static final String APPLICATION_NAME = "pi-reporting-app";

    private static final Log LOG = LogFactory.getLog(ReportingApplication.class);
    private static final String DELIVER_S_S = "deliver(%s, %s); received message count: %d, sent message count: %d";
    private static final String TOPIC = "topic:report";

    private AtomicInteger messagesReceivedCount;
    private AtomicInteger messagesSentCount;

    private DhtCache dhtCache;
    private ThreadPoolTaskExecutor taskExecutor;

    private Map<String, ReportHandler<ReportableEntity<?>>> reportHandlers;

    public ReportingApplication() {
        messagesReceivedCount = new AtomicInteger();
        messagesSentCount = new AtomicInteger();
        taskExecutor = null;
        dhtCache = null;
        reportHandlers = new HashMap<String, ReportHandler<ReportableEntity<?>>>();
    }

    @Override
    protected NodeScope getSuperNodeTopicScope() {
        return NodeScope.AVAILABILITY_ZONE;
    }

    @Override
    protected String getSuperNodeTopicUrl() {
        return TOPIC;
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Resource(name = "generalCache")
    public void setDhtCache(DhtCache aDhtCache) {
        dhtCache = aDhtCache;
    }

    @Resource
    public void setReportHandlers(List<ReportHandler<ReportableEntity<?>>> theReportHandlers) {
        for (ReportHandler<ReportableEntity<?>> reportHandler : theReportHandlers) {
            LOG.info(String.format("adding %s", reportHandler.getClass().getSimpleName()));
            for (String reportableEntityTypeHandled : reportHandler.getReportableEntityTypesHandled())
                if (reportHandlers.put(reportableEntityTypeHandled, reportHandler) != null)
                    throw new IllegalArgumentException(String.format("Multiple report handlers found for %s", reportableEntityTypeHandled));
        }
    }

    @Resource
    public void setThreadPoolTaskExecutor(ThreadPoolTaskExecutor aTaskExecutor) {
        taskExecutor = aTaskExecutor;
    }

    @Override
    public void deliver(PubSubMessageContext context, EntityMethod entityMethod, PiEntity data) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(DELIVER_S_S, context, data, messagesReceivedCount.incrementAndGet(), messagesSentCount.get()));

        deliverUpdateToReportHandler(data, true);
    }

    @SuppressWarnings("unchecked")
    private void deliverUpdateToReportHandler(PiEntity data, boolean fromSupernode) {
        ReportHandler<ReportableEntity<?>> reportHandler = getReportHandler(data);
        if (data instanceof PiEntityCollection<?>)
            reportHandler.receive((PiEntityCollection<ReportableEntity<?>>) data, fromSupernode);
        else if (data instanceof ReportableEntity<?>)
            reportHandler.receive((ReportableEntity<?>) data);
        else
            LOG.warn(String.format("Received update for unexpected entity: %s", data.getType()));
    }

    private ReportHandler<ReportableEntity<?>> getReportHandler(PiEntity data) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("getReportHandler(" + data + ")");
        }
        ReportHandler<ReportableEntity<?>> reportHandler = reportHandlers.get(data.getType());
        if (reportHandler == null) {
            String message = String.format("No report handlers found to deal with data type: %s", data.getType());
            LOG.warn(message);
            throw new IllegalArgumentException(message);
        }
        return reportHandler;
    }

    @Override
    public void deliver(PId id, final ReceivedMessageContext receivedMessageContext) {
        LOG.debug(String.format(DELIVER_S_S, id.toStringFull(), receivedMessageContext, messagesReceivedCount.incrementAndGet(), messagesSentCount.get()));
        final PiEntity receivedEntity = receivedMessageContext.getReceivedEntity();
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("received entity: %s", receivedEntity));

        if (EntityMethod.UPDATE.equals(receivedMessageContext.getMethod())) {
            LOG.debug("Updating health entities");
            deliverUpdateToReportHandler(receivedEntity, false);
        } else if (EntityMethod.GET.equals(receivedMessageContext.getMethod())) {
            taskExecutor.execute(new Runnable() {
                @SuppressWarnings("rawtypes")
                @Override
                public void run() {
                    PiEntityCollection receivedCollection = (PiEntityCollection) receivedEntity;
                    PiEntityCollection responseCollection = null;
                    ReportHandler<ReportableEntity<?>> reportHandler = getReportHandler(receivedEntity);
                    if (CollectionUtils.isEmpty(receivedCollection.getEntities())) {
                        LOG.debug("Getting all entities");
                        responseCollection = reportHandler.getAllEntities();

                    } else {
                        LOG.debug("Getting only the set of entities requested by user");
                        responseCollection = reportHandler.getEntities(receivedCollection);
                    }
                    receivedMessageContext.sendResponse(EntityResponseCode.OK, responseCollection);

                }
            });
        }
    }

    public void publishToReportingTopic(PiEntity entity) {
        LOG.debug(String.format("publishToReportingTopic(%s)", entity.getClass()));
        newPubSubMessageContext(getSuperNodeTopicId(), UUID.randomUUID().toString()).publishContent(EntityMethod.UPDATE, entity);

        messagesSentCount.incrementAndGet();
    }

    public void sendReportingUpdateToASuperNode(final PiEntity piEntity) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("sendReportingUpdateToASuperNode(%s)", piEntity));

        PId superNodeApplicationCheckPointsId = getKoalaIdFactory().buildPId(SuperNodeApplicationCheckPoints.URL);
        dhtCache.get(superNodeApplicationCheckPointsId, new PiContinuation<SuperNodeApplicationCheckPoints>() {
            @Override
            public void handleResult(SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints) {
                if (superNodeApplicationCheckPoints == null) {
                    LOG.debug("Super nodes not seeded, so no need to publish updates");
                    return;
                }

                String randomSuperNodeCheckPoint = superNodeApplicationCheckPoints.getRandomSuperNodeCheckPoint(APPLICATION_NAME, getKoalaIdFactory().getRegion(), getKoalaIdFactory().getAvailabilityZoneWithinRegion());
                if (randomSuperNodeCheckPoint == null || StringUtils.isBlank(randomSuperNodeCheckPoint)) {
                    LOG.debug("Reporting application not seeded as a supernode, so no need to publish updates");
                    return;
                }

                newMessageContext(UUID.randomUUID().toString()).routePiMessage(getKoalaIdFactory().buildPIdFromHexString(randomSuperNodeCheckPoint), EntityMethod.UPDATE, piEntity);

                messagesSentCount.incrementAndGet();
            }
        });
    }

    public int getMessagesReceivedCount() {
        return messagesReceivedCount.intValue();
    }

    public int getMessagesSentCount() {
        return messagesSentCount.intValue();
    }
}

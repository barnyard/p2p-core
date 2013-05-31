package com.bt.pi.core.application.watcher;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.SerialExecutor;

@Component
public class WatcherApplication extends KoalaPastryApplicationBase {
    private static final Log LOG = LogFactory.getLog(WatcherApplication.class);

    private static final int ONE = 1;
    private static final int SIXTY = 60;
    private static final String APPLICATION_NAME = "watcherApplication";

    @Resource
    private AlwaysOnApplicationActivator applicationActivator;
    @Resource
    private SerialExecutor serialExecutor;

    public WatcherApplication() {
        applicationActivator = null;
        serialExecutor = null;
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("deliver(%s, %s)", id, receivedMessageContext));

        PiEntity receivedEntity = receivedMessageContext.getReceivedEntity();
        if (receivedEntity == null) {
            LOG.debug("Null entity received");
            receivedMessageContext.sendResponse(EntityResponseCode.ERROR, null);
            return;
        }

        if (!(receivedEntity instanceof WatcherQueryEntity)) {
            LOG.debug(String.format("Unknown entity type received: %s", receivedEntity.getClass()));
            receivedMessageContext.sendResponse(EntityResponseCode.ERROR, receivedEntity);
            return;
        }

        WatcherQueryEntity watcherQueryEntity = (WatcherQueryEntity) receivedEntity;
        if (serialExecutor.isQueuedOrRunning(watcherQueryEntity.getQueueUrl(), watcherQueryEntity.getEntityUrl())) {
            LOG.debug(String.format("Task with identifier %s is queued or running", watcherQueryEntity));
            receivedMessageContext.sendResponse(EntityResponseCode.OK, watcherQueryEntity);
        } else {
            LOG.debug(String.format("Task with identifier %s is not queued or running", watcherQueryEntity));
            receivedMessageContext.sendResponse(EntityResponseCode.NOT_FOUND, watcherQueryEntity);
        }
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
    }

    @Override
    public boolean becomeActive() {
        return true;
    }

    @Override
    public void becomePassive() {
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return SIXTY;
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public long getStartTimeout() {
        return ONE;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }
}

package com.bt.pi.core.application.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@Component
public class LocalStorageScanningApplication extends KoalaPastryApplicationBase {
    private static final Log LOG = LogFactory.getLog(LocalStorageScanningApplication.class);
    private static final int THIRTY = 30;
    private static final int SIXTY = 60;
    private static final long SCAN_DELAY_MILLIS = THIRTY * 1000;
    private static final String DEFAULT_SLEEP_MILLIS = "200";
    private boolean active;
    @Resource(type = AlwaysOnApplicationActivator.class)
    private ApplicationActivator applicationActivator;
    private List<LocalStorageScanningHandler> handlers;
    private int sleepMillis = Integer.parseInt(DEFAULT_SLEEP_MILLIS);

    public LocalStorageScanningApplication() {
        this.applicationActivator = null;
        this.handlers = new ArrayList<LocalStorageScanningHandler>();
    }

    @Property(key = "local.storage.scanner.sleep.millis", defaultValue = DEFAULT_SLEEP_MILLIS)
    public void setSleepMillis(int value) {
        this.sleepMillis = value;
    }

    @SuppressWarnings("unchecked")
    @Scheduled(fixedDelay = SCAN_DELAY_MILLIS)
    public void scan() {
        if (!active) {
            LOG.debug("not active");
            return;
        }
        Set<Entry<Id, KoalaGCPastMetadata>> entrySet = getPersistentDhtStorage().scanMetadata().entrySet();
        LOG.debug(String.format("scanning %d entries from local storage", entrySet.size()));
        for (Entry<Id, KoalaGCPastMetadata> entry : entrySet) {
            for (LocalStorageScanningHandler handler : handlers) {
                callHandler(handler, entry.getKey(), entry.getValue());
                sleep();
            }
        }
    }

    private void callHandler(LocalStorageScanningHandler handler, Id id, KoalaGCPastMetadata metadata) {
        try {
            handler.handle(id, metadata);
        } catch (Throwable t) {
            LOG.error(String.format("handler %s threw an Exception", handler.getClass().getSimpleName()), t);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            LOG.error("sleep interrupted", e);
        }
        // TODO: should we also yield here?
    }

    @Resource
    public void setHandlers(List<LocalStorageScanningHandler> theHandlers) {
        this.handlers = theHandlers;
    }

    @Override
    public String getApplicationName() {
        return getClass().getSimpleName();
    }

    @Override
    public int getActivationCheckPeriodSecs() {
        return THIRTY;
    }

    @Override
    public long getStartTimeout() {
        return SIXTY;
    }

    @Override
    public TimeUnit getStartTimeoutUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public boolean becomeActive() {
        active = true;
        return true;
    }

    @Override
    public void becomePassive() {
        active = false;
    }

    @Override
    public void handleNodeDeparture(String nodeId) {
    }

    @Override
    public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
    }

    @Override
    public ApplicationActivator getApplicationActivator() {
        return applicationActivator;
    }

    public PersistentDhtStorage getPersistentDhtStorage() {
        return super.getPersistentDhtStorage();
    }
}

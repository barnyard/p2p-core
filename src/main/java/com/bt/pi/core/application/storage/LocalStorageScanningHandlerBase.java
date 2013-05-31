package com.bt.pi.core.application.storage;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

public abstract class LocalStorageScanningHandlerBase implements LocalStorageScanningHandler {
    private static final Log LOG = LogFactory.getLog(LocalStorageScanningHandlerBase.class);
    @Resource
    private KoalaIdUtils koalaIdUtils;
    @Resource
    private DhtClientFactory dhtClientFactory;
    @Resource
    private KoalaIdFactory koalaIdFactory;
    @Resource
    private ReportingApplication reportingApplication;

    @Override
    public void handle(Id id, KoalaGCPastMetadata metadata) {
        LOG.debug(String.format("handle(%s, %s)", id.toStringFull(), metadata));
        if (metadata.isDeletedAndDeletable())
            return;
        if (!getEntityType().equals(metadata.getEntityType()))
            return;
        if (!iAmTheOwnerNode(id))
            return;
        doHandle(id, metadata);
    }

    protected abstract void doHandle(Id id, KoalaGCPastMetadata metadata);

    protected abstract String getEntityType();

    private boolean iAmTheOwnerNode(Id id) {
        LOG.debug(String.format("iAmTheOwnerNode(%s)", id));
        return koalaIdUtils.isIdClosestToMe(reportingApplication.getNodeIdFull(), reportingApplication.getLeafNodeHandles(), id);
    }

    protected ReportingApplication getReportingApplication() {
        return reportingApplication;
    }

    protected KoalaIdFactory getKoalaIdFactory() {
        return koalaIdFactory;
    }

    protected DhtClientFactory getDhtClientFactory() {
        return dhtClientFactory;
    }
}

package com.bt.pi.core.past;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdFactory;
import rice.p2p.commonapi.IdSet;
import rice.p2p.past.gc.GCId;
import rice.p2p.past.gc.GCPastMetadata;
import rice.p2p.replication.ReplicationPolicy;
import rice.persistence.StorageManager;

import com.bt.pi.core.id.KoalaIdUtils;

public class KoalaReplicationPolicy implements ReplicationPolicy {
    private static final Log LOG = LogFactory.getLog(KoalaReplicationPolicy.class);
    private StorageManager storageManager;

    public KoalaReplicationPolicy(StorageManager storage) {
        LOG.debug(String.format("KoalaReplicationPolicy(%s)", storage));
        this.storageManager = storage;
    }

    /**
     * This method simply returns remote-local.
     * 
     * @param local
     *            The set of local ids
     * @param remote
     *            The set of remote ids
     * @param factory
     *            The factory to use to create IdSets
     * @return A subset of the remote ids which need to be fetched
     */
    public IdSet difference(IdSet local, IdSet remote, IdFactory factory) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("difference(%s, %s, %s)", local, remote, factory));
        IdSet result = factory.buildIdSet();
        Iterator<Id> i = remote.getIterator();
        while (i.hasNext()) {
            Id id = (Id) i.next();
            if (LOG.isDebugEnabled())
                LOG.debug("Id: " + id.toStringFull());
            if (isBackupId(id)) {
                if (LOG.isDebugEnabled())
                    LOG.debug("backup!");
                continue;
            }
            if (!local.isMemberId(id))
                result.addId(id);
            else {
                long localVersion = getLocalVersion(id);
                long remoteVersion = getRemoteVersion(id);
                if (remoteVersion > localVersion)
                    result.addId(id);
            }
        }
        if (LOG.isDebugEnabled())
            LOG.debug("returning " + result);
        return result;
    }

    private boolean isBackupId(Id id) {
        Id theId = id;
        if (id instanceof GCId)
            theId = ((GCId) id).getId();
        return KoalaIdUtils.isBackupId(theId);
    }

    private long getRemoteVersion(Id id) {
        if (id instanceof GCId) {
            GCId gcId = (GCId) id;
            return gcId.getExpiration();
        }
        return -1;
    }

    private long getLocalVersion(Id id) {
        if (id instanceof GCId) {
            GCId gcId = (GCId) id;
            GCPastMetadata metadata = (GCPastMetadata) storageManager.getMetadata(gcId.getId());
            if (null == metadata)
                return -1;
            return metadata.getExpiration();
        }
        return -1;
    }
}

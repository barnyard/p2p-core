//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.past;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.PastPolicy;
import rice.persistence.Cache;

import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.past.content.KoalaContentHandleBase;
import com.bt.pi.core.past.continuation.KoalaHandleVersionComparer;

public class KoalaPastPolicy implements PastPolicy {
    private static final Log LOG = LogFactory.getLog(KoalaPastPolicy.class);
    private KoalaHandleVersionComparer koalaHandleVersionComparer = new KoalaHandleVersionComparer();

    public boolean allowInsert(PastContent content) {
        return true;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    public void fetch(final Id id, final NodeHandle hint, Cache backup, final Past past, Continuation command) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("fetch(Id - %s, NodeHandle - %s, Cache - %s, Past - %s,Continuation - %s)", id.toStringFull(), hint, backup, past, command));
        past.lookupHandles(id, past.getReplicationFactor() + 1, new StandardContinuation(command) {
            public void receiveResult(Object o) {
                PastContentHandle[] handles = (PastContentHandle[]) o;
                if (LOG.isDebugEnabled())
                    LOG.debug("Returned Handles for id: " + Arrays.toString(handles));
                if ((handles == null) || (handles.length == 0)) {
                    parent.receiveException(new KoalaException("Unable to fetch data - returned unexpected null."));
                    return;
                }

                KoalaContentHandleBase handle = koalaHandleVersionComparer.getLatestVersion(handles);

                if (LOG.isDebugEnabled())
                    LOG.debug("Handle: " + handle + " selected.");
                if (handle != null) {
                    if (LOG.isDebugEnabled())
                        LOG.debug(handle.getId().toStringFull());
                    past.fetch(handle, parent);
                } else
                    past.lookupHandle(id, hint, new StandardContinuation(parent) {
                        public void receiveResult(Object o) {
                            if (o != null)
                                past.fetch((PastContentHandle) o, parent);
                            else
                                parent.receiveResult(null);
                        }
                    });
            }
        });
    }

    // for testing
    protected void setKoalaHandleVersionComparer(KoalaHandleVersionComparer aKoalaHandleVersionComparer) {
        this.koalaHandleVersionComparer = aKoalaHandleVersionComparer;
    }
}

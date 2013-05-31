package com.bt.pi.core.past.continuation;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.past.PastContentHandle;

import com.bt.pi.core.past.content.KoalaContentHandleBase;

public class KoalaHandleVersionComparer {
    private static final Log LOG = LogFactory.getLog(KoalaHandleVersionComparer.class);

    public KoalaHandleVersionComparer() {
    }

    public KoalaContentHandleBase getLatestVersion(PastContentHandle[] handles) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("getLatestVersion(PastContentHandle[] - %s )", Arrays.toString(handles)));
        KoalaContentHandleBase handleToReturn = null;

        for (int i = 0; i < handles.length; i++) {
            if (handles[i] instanceof KoalaContentHandleBase) {
                KoalaContentHandleBase handle = (KoalaContentHandleBase) handles[i];
                handleToReturn = getLatestHandle(handleToReturn, handle);
            }
        }
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Latest version found. Returning - %s )", handleToReturn));
        return handleToReturn;
    }

    private KoalaContentHandleBase getLatestHandle(KoalaContentHandleBase handleA, KoalaContentHandleBase handleB) {
        // so to surmise this if. :D
        /*
         * 1. If nothing is set.. set the handle. 2. Take the higher version. 3.
         * If the versions are equal take the first handle set with that
         * version.
         */
        KoalaContentHandleBase handleToReturn = handleA;
        if (handleB != null && handleA == null) {
            handleToReturn = handleB;
        } else if (handleA != null && handleB != null) {
            if (handleB.getVersion() > handleA.getVersion()) {
                handleToReturn = handleB;
            } else if (handleB.getVersion() == handleA.getVersion() && handleB.getTimeStamp() < handleA.getTimeStamp()) {
                handleToReturn = handleB;
            }
        }
        return handleToReturn;
    }
}

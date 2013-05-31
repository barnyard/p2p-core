package com.bt.pi.core.past.continuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.p2p.past.Past;
import rice.p2p.past.PastContentHandle;

import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.past.content.KoalaContentHandleBase;

public class KoalaFreshestHandleLookUpHandlesContinuation extends LoggingContinuationWithKoalaHandleVersionComparer<Object> {
    private static final Log LOG = LogFactory.getLog(KoalaFreshestHandleLookUpHandlesContinuation.class);
    private Continuation<Object, Exception> applicationContinuation;
    private Past past;

    @SuppressWarnings("unchecked")
    public KoalaFreshestHandleLookUpHandlesContinuation(Past koalaPast, Continuation responseContinuation) {
        past = koalaPast;
        applicationContinuation = responseContinuation;
    }

    @Override
    protected void receiveExceptionInternal(Exception exception) {
        applicationContinuation.receiveException(exception);
    }

    @Override
    protected void receiveResultInternal(Object result) {
        try {
            PastContentHandle[] handles = (PastContentHandle[]) result;
            KoalaContentHandleBase handle = getLatestVersion(handles);
            if (LOG.isDebugEnabled())
                LOG.debug("Newest version of Handle: " + handle);
            if (handle == null) {
                applicationContinuation.receiveResult(null);
            } else
                past.fetch(handle, applicationContinuation);
        } catch (KoalaException e) {
            receiveException(e);
        }
    }
}

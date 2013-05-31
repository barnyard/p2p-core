package com.bt.pi.core.past.continuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.p2p.past.PastContent;

import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaPiEntityContent;

public class KoalaPiEntityResultContinuation<T extends PiEntity> extends GenericContinuation<PastContent> {
    private static final Log LOG = LogFactory.getLog(KoalaPiEntityResultContinuation.class);
    private Continuation<T, Exception> piEntityContinuation;
    private KoalaPiEntityFactory koalaPiEntityFactory;

    public KoalaPiEntityResultContinuation(Continuation<T, Exception> continuation, KoalaPiEntityFactory piEntityFactory) {
        piEntityContinuation = continuation;
        koalaPiEntityFactory = piEntityFactory;
    }

    @Override
    public void handleException(Exception exception) {
        piEntityContinuation.receiveException(exception);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleResult(PastContent result) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("receiveResult(PastContent %s) - ParentContinuation: %s", result, piEntityContinuation));
        if (result == null) {
            piEntityContinuation.receiveResult((T) null);
        } else if (result instanceof KoalaPiEntityContent) {
            KoalaPiEntityContent content = (KoalaPiEntityContent) result;
            try {
                PiEntity entity = koalaPiEntityFactory.getPiEntity(content.getBody());
                piEntityContinuation.receiveResult((T) entity);
            } catch (Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.debug(String.format("Caught exception of type %s with message %s - delegating to continuation exception handler", e.getClass(), e.getMessage()));
                receiveException(e);
            }
        } else
            receiveException(new KoalaException("Content object is not a PiEntity. Object: " + result));
    }

}

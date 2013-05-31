package com.bt.pi.core.past.internalcontinuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.past.PastImpl.MessageBuilder;
import rice.p2p.past.messaging.PastMessage;

import com.bt.pi.core.continuation.ContinuationUtils;

public class KoalaPastGetHandlesInsertContinuation extends StandardContinuation<Object, Exception> {
    private static final String UNCHECKED = "unchecked";
    private static final Log LOG = LogFactory.getLog(KoalaPastGetHandlesInsertContinuation.class);
    private PiGenericThresholdSensitiveMultiContinuation multi;
    private KoalaPast past;
    private MessageBuilder builder;
    private boolean useSocket;
    private Id id;
    private Object localResult;

    @SuppressWarnings(UNCHECKED)
    public KoalaPastGetHandlesInsertContinuation(KoalaPast p, Continuation continuation, Id contentId, MessageBuilder messageBuilder, boolean shouldUseSocket) {
        super(continuation);
        past = p;
        builder = messageBuilder;
        useSocket = shouldUseSocket;
        id = contentId;
    }

    public void setlocalResult(Object o) {
        localResult = o;
    }

    @SuppressWarnings(UNCHECKED)
    @Override
    public void receiveResult(Object o) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_RESULT, null);
        try {
            NodeHandleSet replicas = (NodeHandleSet) o;
            if (LOG.isDebugEnabled())
                LOG.debug("Received replicas " + replicas + " for id " + id);

            // then we send inserts to each replica and wait for at least
            // threshold * num to return successfully
            multi = new PiGenericThresholdSensitiveMultiContinuation(parent, replicas.size(), past.getSuccessfulInsertThreshold());

            for (int i = 0; i < replicas.size(); i++) {
                NodeHandle handle = replicas.getHandle(i);
                if (LOG.isDebugEnabled())
                    LOG.debug("Processing handle: " + handle);
                if (!handle.equals(past.getLocalNodeHandle())) {
                    PastMessage m = builder.buildMessage();
                    Continuation c = new NamedContinuation("InsertMessage to " + replicas.getHandle(i) + " for " + id, multi.getSubContinuation(i));
                    if (useSocket) {
                        // sendViaSocket(handle, m, c);
                        LOG.error("Error: Send via socket is currently not allowed do to those bumble heads!");
                    } else {
                        sentPastRequest(handle, m, c);
                    }
                } else {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Setting the local result for handle: " + handle + " result " + localResult);
                    multi.setResult(i, localResult);
                }
            }
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_RESULT, null, startTime);
        }
    }

    @SuppressWarnings(UNCHECKED)
    private void sentPastRequest(NodeHandle handle, PastMessage m, Continuation c) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Sending a request via past. handle: %s. message: %s. continuation: %s.", handle, m, c));

        past.sendPastRequest(handle, m, c);
    }

    @Override
    public void receiveException(Exception result) {
        long startTime = ContinuationUtils.logStartOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, null);
        try {
            super.receiveException(result);
        } finally {
            ContinuationUtils.logEndOfContinuation(ContinuationUtils.RECEIVE_EXCEPTION, null, startTime);
        }
    }
}

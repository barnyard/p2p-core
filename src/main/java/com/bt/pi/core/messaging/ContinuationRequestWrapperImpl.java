//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import rice.Continuation;

import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.util.MDCHelper;

@Component
@Scope("prototype")
public class ContinuationRequestWrapperImpl extends RequestWrapperBase<Continuation<KoalaMessage, Exception>, KoalaMessage> implements ContinuationRequestWrapper {
    protected static final String DEFAULT_REQUEST_TIME_TO_LIVE_MILLIS = "3600000";
    protected static final String DEFAULT_HOUSEKEEPING_INTERVAL_SEC = "3600";
    protected static final String CONTINUATION_WRAPPER_REQUEST_TIME_TO_LIVE_MILLIS = "continuation.wrapper.request.time.to.live.millis";
    protected static final String CONTINUATION_WRAPPER_HOUSEKEEPING_INTERVAL_SEC = "continuation.wrapper.housekeeping.interval.sec";
    private static final long MILLISECONDS_IN_A_SECOND = 1000L;
    private static final Log LOG = LogFactory.getLog(ContinuationRequestWrapperImpl.class);
    private ConcurrentMap<String, RequestState<Continuation<KoalaMessage, Exception>>> continuationMap;
    private WatcherService watcherService;
    private int messageTimeToLiveMillis;
    private long housekeepIntervalMillis;

    public ContinuationRequestWrapperImpl() {
        super();
        watcherService = null;
        continuationMap = new ConcurrentHashMap<String, RequestState<Continuation<KoalaMessage, Exception>>>();
        this.housekeepIntervalMillis = Long.parseLong(DEFAULT_HOUSEKEEPING_INTERVAL_SEC) * MILLISECONDS_IN_A_SECOND;
        this.messageTimeToLiveMillis = Integer.parseInt(DEFAULT_REQUEST_TIME_TO_LIVE_MILLIS);
    }

    @Resource
    public void setWatcherService(WatcherService aWatcherService) {
        watcherService = aWatcherService;
    }

    protected Map<String, RequestState<Continuation<KoalaMessage, Exception>>> getContinuationMap() {
        return continuationMap;
    }

    @Property(key = CONTINUATION_WRAPPER_HOUSEKEEPING_INTERVAL_SEC, defaultValue = DEFAULT_HOUSEKEEPING_INTERVAL_SEC)
    public void setHousekeepIntervalMillis(int value) {
        this.housekeepIntervalMillis = value * MILLISECONDS_IN_A_SECOND;
    }

    @PostConstruct
    public void initialise() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                housekeepStaleMessages();
            }
        };

        LOG.debug(String.format("Starting continuation wrapper watcher for %s with interval %d ms", this, housekeepIntervalMillis));
        watcherService.addTask(this.toString(), runnable, housekeepIntervalMillis, housekeepIntervalMillis);
    }

    @Override
    public boolean messageReceived(PId id, KoalaMessage message) {
        LOG.debug(String.format("messageReceived(%s, %s)", id, message.getClass().getSimpleName()));
        MDCHelper.putTransactionUID(message.getTransactionUID());

        boolean continuationProcessed = false;
        LOG.debug(String.format("messageReceived(Id - %s,KoalaMessage - %s)", id, message));
        if (StringUtils.isNotBlank(message.getCorrelationUID())) {
            continuationProcessed = processMessageForContinuation(message);
        }

        MDCHelper.clearTransactionUID();
        return continuationProcessed;
    }

    private boolean processMessageForContinuation(KoalaMessage message) {
        if (message.getResponseCode() == null)
            return false;

        boolean continuationProcessed = false;
        RequestState<Continuation<KoalaMessage, Exception>> continuationState = continuationMap.get(message.getCorrelationUID());
        if (continuationState != null) {
            continuationState.getResponse().receiveResult(message);
            continuationMap.remove(message.getCorrelationUID());
            continuationProcessed = true;
        }
        return continuationProcessed;
    }

    @Override
    public void sendRequest(PId id, KoalaMessage message, KoalaMessageSender messageSender, Continuation<KoalaMessage, Exception> continuation) {
        LOG.debug(String.format("sendRequest(Id - %s,KoalaMessage - %s, Sender - %s, Continuation - %s)", id, message, messageSender, continuation));
        if (continuation == null || StringUtils.isNotBlank(message.getCorrelationUID())) {
            RequestState<Continuation<KoalaMessage, Exception>> continuationRequestState = new RequestState<Continuation<KoalaMessage, Exception>>();
            continuationRequestState.setResponse(continuation);
            continuationMap.put(message.getCorrelationUID(), continuationRequestState);
        } else
            LOG.warn(String.format("sendRequest called with message: %s  to Id: %s containing no continuation or CorrelationUID. Skipping adding message to hash", id, message));

        messageSender.routeMessage(id, message);
    }

    @Property(key = CONTINUATION_WRAPPER_REQUEST_TIME_TO_LIVE_MILLIS, defaultValue = DEFAULT_REQUEST_TIME_TO_LIVE_MILLIS)
    public void setMessageTimeToLiveMillis(int value) {
        this.messageTimeToLiveMillis = value;
    }

    protected void housekeepStaleMessages() {
        LOG.debug(String.format("housekeepStaleMessages()"));
        List<String> messagesToHousekeep = new ArrayList<String>();
        for (Entry<String, RequestState<Continuation<KoalaMessage, Exception>>> entry : continuationMap.entrySet()) {
            RequestState<Continuation<KoalaMessage, Exception>> current = entry.getValue();
            long now = getNow();
            if (current.getCreationTimestamp() + messageTimeToLiveMillis < now) {
                messagesToHousekeep.add(entry.getKey());
            }
        }

        for (String key : messagesToHousekeep) {
            LOG.debug(String.format("Removing stale message with correlation id %s", key));
            continuationMap.remove(key);
        }
    }

    protected long getNow() {
        return System.currentTimeMillis();
    }
}

package com.bt.pi.core.application.resource.watched;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.id.PId;

public class FiniteLifespanConsumerStatusChecker {
    private static final Log LOG = LogFactory.getLog(FiniteLifespanConsumerStatusChecker.class);
    private String consumerId;
    private DhtClientFactory dhtClientFactory;
    private PId consumerRecordId;

    public FiniteLifespanConsumerStatusChecker(String aConsumerId, PId aConsumerRecordId, DhtClientFactory aDhtClientFactory) {
        consumerId = aConsumerId;
        dhtClientFactory = aDhtClientFactory;
        consumerRecordId = aConsumerRecordId;
    }

    protected String getConsumerId() {
        return consumerId;
    }

    public <T extends FiniteLifespanEntity> void check(final FiniteLifespanConsumerCheckCallback<T> consumerActiveCallback, final FiniteLifespanConsumerCheckCallback<T> consumerInactiveCallback) {
        LOG.info(String.format("check(%s, %s, %s)", consumerRecordId, consumerActiveCallback, consumerInactiveCallback));
        DhtReader dhtReader = dhtClientFactory.createReader();
        dhtReader.getAsync(consumerRecordId, new PiContinuation<T>() {
            @Override
            public void handleResult(T entity) {
                if (entity == null || entity.isDead()) {
                    LOG.debug(String.format("Null or dead consumer entity: %s", entity));
                    if (consumerInactiveCallback != null)
                        consumerInactiveCallback.handleCallback(entity);
                } else {
                    LOG.debug(String.format("Active consumer entity: %s", entity));
                    if (consumerActiveCallback != null)
                        consumerActiveCallback.handleCallback(entity);
                }
            }
        });
    }
}

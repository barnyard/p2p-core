package com.bt.pi.core.application.resource.leased;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.util.annotation.AnnotationUtils;

@Component
public class LeasedResourceAllocationRecordHeartbeater {
    private static final Log LOG = LogFactory.getLog(LeasedResourceAllocationRecordHeartbeater.class);
    private DhtClientFactory dhtClientFactory;
    private KoalaIdFactory koalaIdFactory;

    public LeasedResourceAllocationRecordHeartbeater() {
        dhtClientFactory = null;
        koalaIdFactory = null;
    }

    @Resource
    public void setDhtClientFactory(DhtClientFactory aDhtClientFactory) {
        this.dhtClientFactory = aDhtClientFactory;
    }

    @Resource
    public void setKoalaIdFactory(KoalaIdFactory aKoalaIdFactory) {
        this.koalaIdFactory = aKoalaIdFactory;
    }

    public <EntityType> void timestampLeasedAllocatedResources(EntityType entity) {
        LOG.debug(String.format("timestampLeasedAllocatedResources(%s)", entity));

        if (null == entity) {
            LOG.debug("Entity is null");
            return;
        }

        List<Method> leasedAllocatedResourceMethods = AnnotationUtils.findAnnotatedMethods(entity.getClass(), LeasedAllocatedResource.class);
        for (Method m : leasedAllocatedResourceMethods) {
            Class<?> resourceTypeClazz = m.getReturnType();
            final LeasedAllocatedResource leasedAllocatedResource = m.getAnnotation(LeasedAllocatedResource.class);
            PId recordId = koalaIdFactory.buildPId(leasedAllocatedResource.allocationRecordUri()).forLocalScope(leasedAllocatedResource.allocationRecordScope());
            try {
                timestampLeasedAllocatedResource(recordId, entity, m, resourceTypeClazz);
            } catch (Throwable t) {
                LOG.error(String.format("Error timestamping leased allocated resource %s in %s", m.getName(), entity), t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> void timestampLeasedAllocatedResource(PId recordId, Object entity, Method resourceGetterMethod, Class<T> resourceTypeClazz) throws Exception {
        T resourceId = (T) resourceGetterMethod.invoke(entity);

        List<Method> consumerIdMethods = AnnotationUtils.findAnnotatedMethods(entity.getClass(), LeasedAllocatedResourceConsumerId.class);
        if (consumerIdMethods.size() != 1) {
            throw new IncorrectNumberOfAnnotationsException(String.format("Expected exactly one method annotated with %s, but got %d", LeasedAllocatedResourceConsumerId.class.getSimpleName(), consumerIdMethods.size()));
        }

        String consumerId = consumerIdMethods.get(0).invoke(entity).toString();

        List<T> resourceIds = new ArrayList<T>();
        resourceIds.add(resourceId);

        List<String> consumerIds = new ArrayList<String>();
        consumerIds.add(consumerId);

        heartbeat(recordId, resourceIds, consumerIds);
    }

    public <ResourceIdType> void heartbeat(final PId recordId, final List<ResourceIdType> resources, final List<String> consumerIds) {
        LOG.debug(String.format("heartbeat(%s, %s, %s)", recordId.toStringFull(), resources, consumerIds));
        DhtWriter dhtWriter = dhtClientFactory.createWriter();
        dhtWriter.update(recordId, new UpdateResolvingPiContinuation<LeasedResourceAllocationRecord<ResourceIdType>>() {
            @Override
            public LeasedResourceAllocationRecord<ResourceIdType> update(LeasedResourceAllocationRecord<ResourceIdType> existingEntity, LeasedResourceAllocationRecord<ResourceIdType> requestedEntity) {
                boolean updated = false;
                for (int i = 0; i < resources.size(); i++) {
                    ResourceIdType resourceId = resources.get(i);
                    String consumerId = consumerIds.get(i);
                    boolean heartbeatCompleted = existingEntity.heartbeat(resourceId, consumerId);
                    if (heartbeatCompleted) {
                        LOG.debug(String.format("Heartbeat allocation of resource %s for consumer %s", resourceId, consumerId));
                        updated = true;
                    } else {
                        LOG.warn(String.format("Did not heartbeat allocation of resource %s for consumer %s", resourceId, consumerId));
                    }
                }
                if (updated)
                    return existingEntity;
                else
                    return null;
            }

            @Override
            public void handleResult(LeasedResourceAllocationRecord<ResourceIdType> result) {
                LOG.debug(String.format("Heartbeat for consumers %s of resources %s in record %s returned %s", consumerIds, resources, recordId, result));
            }
        });
    }
}

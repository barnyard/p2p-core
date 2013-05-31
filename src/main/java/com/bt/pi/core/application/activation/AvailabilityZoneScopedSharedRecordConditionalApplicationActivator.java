package com.bt.pi.core.application.activation;

import org.springframework.stereotype.Component;

import com.bt.pi.core.scope.NodeScope;

/**
 * An implementation of {@link SharedRecordConditionalApplicationActivator} where the ApplicationRecord used for
 * coordination is availabiltyzone scoped.
 * 
 * @see SharedRecordConditionalApplicationActivator
 * 
 */
@Component
public class AvailabilityZoneScopedSharedRecordConditionalApplicationActivator extends SharedRecordConditionalApplicationActivator {
    public AvailabilityZoneScopedSharedRecordConditionalApplicationActivator() {
    }

    @Override
    public NodeScope getActivationScope() {
        return NodeScope.AVAILABILITY_ZONE;
    }
}

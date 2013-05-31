package com.bt.pi.core.application.activation;

import org.springframework.stereotype.Component;

import com.bt.pi.core.scope.NodeScope;

/**
 * An implementation of {@link SharedRecordConditionalApplicationActivator} where the ApplicationRecord used for
 * coordination is region scoped.
 * 
 * @see SharedRecordConditionalApplicationActivator
 * 
 */
@Component
public class RegionScopedSharedRecordConditionalApplicationActivator extends SharedRecordConditionalApplicationActivator {
    public RegionScopedSharedRecordConditionalApplicationActivator() {
    }

    @Override
    public NodeScope getActivationScope() {
        return NodeScope.REGION;
    }
}

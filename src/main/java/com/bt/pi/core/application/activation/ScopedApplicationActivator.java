package com.bt.pi.core.application.activation;

import com.bt.pi.core.scope.NodeScope;

public interface ScopedApplicationActivator extends ApplicationActivator {

    /**
     * The scope (global, region, availability zone) to which activation relates - eg have 3 apps active per region
     */
    NodeScope getActivationScope();
}

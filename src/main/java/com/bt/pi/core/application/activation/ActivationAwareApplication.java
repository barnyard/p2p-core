package com.bt.pi.core.application.activation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

/**
 * Consider using {ActivationAwareApplicationBase} or one of its subtypes before implementing this interface directly.
 */
public interface ActivationAwareApplication {

    Id getNodeId();

    Collection<NodeHandle> getLeafNodeHandles();

    /**
     * The unique identifier for your application Defaults to "pi-ClassName"
     */
    String getApplicationName();

    /**
     * Applications you cannot run on the same node as. Defaults to an empty List - override if required.
     */
    List<String> getPreferablyExcludedApplications();

    /**
     * How frequently the ApplicationActivator is called
     */
    int getActivationCheckPeriodSecs();

    /**
     * THe maximum time to wait before deeming the application to have failed to start.
     */
    long getStartTimeout();

    TimeUnit getStartTimeoutUnit();

    /**
     * @return true if the application has become active
     */
    boolean becomeActive();

    void becomePassive();

    ApplicationActivator getApplicationActivator();
}

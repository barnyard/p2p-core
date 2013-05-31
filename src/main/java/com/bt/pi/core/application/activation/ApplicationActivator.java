package com.bt.pi.core.application.activation;

public interface ApplicationActivator {
    void register(ActivationAwareApplication anActivationAwareApplication);

    ApplicationStatus getApplicationStatus(String appName);

    void deActivateNode(String id, ActivationAwareApplication anActivationAwareApplication);
}
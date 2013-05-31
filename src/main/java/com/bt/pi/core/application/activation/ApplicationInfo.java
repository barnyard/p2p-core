package com.bt.pi.core.application.activation;


public class ApplicationInfo {
    private ActivationAwareApplication application;
    private ApplicationStatus applicationStatus;
    private ApplicationRecord cachedApplicationRecord;

    public ApplicationInfo(ActivationAwareApplication activationAwareApplication) {
        this.applicationStatus = ApplicationStatus.NOT_INITIALIZED;
        this.cachedApplicationRecord = null;
        this.application = activationAwareApplication;
    }

    public ActivationAwareApplication getApplication() {
        return application;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus anApplicationStatus) {
        this.applicationStatus = anApplicationStatus;
    }

    public ApplicationRecord getCachedApplicationRecord() {
        return cachedApplicationRecord;
    }

    public void setCachedApplicationRecord(ApplicationRecord aCachedApplicationRecord) {
        this.cachedApplicationRecord = aCachedApplicationRecord;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ApplicationInfo [application=").append(application).append(", applicationStatus=").append(applicationStatus).append(", cachedApplicationRecord=").append(cachedApplicationRecord).append("]");
        return builder.toString();
    }

}

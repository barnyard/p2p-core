package com.bt.pi.core.application.activation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

/*
 * The ApplicationRegistry contains the status of all applications currently running on the node.
 */
@Component
public class ApplicationRegistry {
    private static final Log LOG = LogFactory.getLog(ApplicationRegistry.class);
    private static final String UNKNOWN_APPLICATION_S = "Unknown application: %s";
    private static final String APPLICATION_S_HAS_NOT_BEEN_REGISTERED = "Application %s has not been registered";
    private ConcurrentHashMap<String, ApplicationInfo> applicationHash;

    public ApplicationRegistry() {
        applicationHash = new ConcurrentHashMap<String, ApplicationInfo>();
    }

    public Set<String> getApplicationNames() {
        return applicationHash.keySet();
    }

    public synchronized void registerApplication(ActivationAwareApplication app) {
        ApplicationInfo existing = applicationHash.putIfAbsent(app.getApplicationName(), new ApplicationInfo(app));
        if (existing != null)
            throw new ApplicationAlreadyExistsException(String.format("Application %s already exists", app.getApplicationName()));
    }

    public synchronized void setApplicationStatus(String applicationName, ApplicationStatus status) {
        ApplicationInfo existing = applicationHash.get(applicationName);
        if (existing == null)
            throw new UnknownApplicationException(String.format(APPLICATION_S_HAS_NOT_BEEN_REGISTERED, applicationName));

        LOG.debug(String.format(this + " Setting app status for app %s to %s", applicationName, status));
        existing.setApplicationStatus(status);
    }

    public synchronized ApplicationInfo getApplicationInfor(String applicationName) {
        ApplicationInfo existing = applicationHash.get(applicationName);
        if (existing == null)
            throw new UnknownApplicationException(String.format(UNKNOWN_APPLICATION_S, applicationName));
        return existing;
    }

    public synchronized ApplicationStatus getApplicationStatus(String applicationName) {
        ApplicationInfo existing = applicationHash.get(applicationName);
        if (existing == null)
            throw new UnknownApplicationException(String.format(UNKNOWN_APPLICATION_S, applicationName));
        return existing.getApplicationStatus();
    }

    public synchronized void setCachedApplicationRecord(String applicationName, ApplicationRecord applicationRecord) {
        ApplicationInfo existing = applicationHash.get(applicationName);
        if (existing == null)
            throw new UnknownApplicationException(String.format(APPLICATION_S_HAS_NOT_BEEN_REGISTERED, applicationName));

        LOG.debug(String.format(this + " Caching app record for %s to %s", applicationName, applicationRecord));
        existing.setCachedApplicationRecord(applicationRecord);
    }

    public synchronized ApplicationRecord getCachedApplicationRecord(String applicationName) {
        ApplicationInfo existing = applicationHash.get(applicationName);
        if (existing == null)
            throw new UnknownApplicationException(String.format(UNKNOWN_APPLICATION_S, applicationName));
        ApplicationRecord res = existing.getCachedApplicationRecord();
        LOG.debug(String.format(this + " Returning cached app record for %s: %s", applicationName, res));
        return res;
    }
}

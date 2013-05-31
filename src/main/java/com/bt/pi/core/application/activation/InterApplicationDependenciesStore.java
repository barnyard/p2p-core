package com.bt.pi.core.application.activation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class InterApplicationDependenciesStore {
    private static final Log LOG = LogFactory.getLog(InterApplicationDependenciesStore.class);
    private Map<String, List<String>> preferablyExcludedApplicationsMap;

    public InterApplicationDependenciesStore() {
        preferablyExcludedApplicationsMap = new HashMap<String, List<String>>();
    }

    @SuppressWarnings("unchecked")
    @Resource
    public void setPastryApplications(List<ActivationAwareApplication> applications) {
        if (applications == null) {
            LOG.warn("Called setPastryApplications with null argument");
            return;
        }
        for (ActivationAwareApplication application : applications) {
            String applicationName = application.getApplicationName();
            if (StringUtils.isEmpty(applicationName))
                throw new IllegalArgumentException("Application name cannot be null or empty");
            List<String> exclusionList = (List<String>) (application.getPreferablyExcludedApplications() == null ? Collections.emptyList() : new ArrayList<String>(application.getPreferablyExcludedApplications()));
            preferablyExcludedApplicationsMap.put(applicationName, exclusionList);
        }
        for (ActivationAwareApplication application : applications) {
            List<String> exclusionListForApplication = (List<String>) (application.getPreferablyExcludedApplications() == null ? Collections.emptyList() : new ArrayList<String>(application.getPreferablyExcludedApplications()));
            for (String excludedApplicationName : exclusionListForApplication) {
                List<String> exclusionListForExcludedApplication = preferablyExcludedApplicationsMap.get(excludedApplicationName);
                if (exclusionListForExcludedApplication != null && !exclusionListForExcludedApplication.contains(application.getApplicationName())) {
                    exclusionListForExcludedApplication.add(application.getApplicationName());
                }
            }
        }
    }

    public List<String> getPreferablyExcludedApplications(String applicationName) {
        return preferablyExcludedApplicationsMap.get(applicationName);
    }
}

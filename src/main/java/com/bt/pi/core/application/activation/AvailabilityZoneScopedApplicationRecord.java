package com.bt.pi.core.application.activation;

import java.util.List;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.AVAILABILITY_ZONE)
public class AvailabilityZoneScopedApplicationRecord extends ApplicationRecord {
    public static final String TYPE = AvailabilityZoneScopedApplicationRecord.class.getSimpleName();
    public static final String URI_SCHEME = "avzapp";

    public AvailabilityZoneScopedApplicationRecord() {
        super();
    }

    public AvailabilityZoneScopedApplicationRecord(String applicationName) {
        super(applicationName);
    }

    public AvailabilityZoneScopedApplicationRecord(String applicationName, long dataVersion) {
        super(applicationName, dataVersion);
    }

    public AvailabilityZoneScopedApplicationRecord(String applicationName, long dataVersion, int requiredActive) {
        super(applicationName, dataVersion, requiredActive);
    }

    public AvailabilityZoneScopedApplicationRecord(String applicationName, long dataVersion, List<String> resources) {
        super(applicationName, dataVersion, resources);
    }

    @Override
    public String getUrl() {
        return AvailabilityZoneScopedApplicationRecord.getUrl(getApplicationName());
    }

    public static String getUrl(String anAppName) {
        return String.format("%s:%s", AvailabilityZoneScopedApplicationRecord.URI_SCHEME, anAppName);
    }

    @Override
    public String getType() {
        return AvailabilityZoneScopedApplicationRecord.TYPE;
    }

    @Override
    public String getUriScheme() {
        return AvailabilityZoneScopedApplicationRecord.URI_SCHEME;
    }
}

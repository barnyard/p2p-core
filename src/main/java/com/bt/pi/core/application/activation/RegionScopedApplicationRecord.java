package com.bt.pi.core.application.activation;

import java.util.List;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.REGION)
public class RegionScopedApplicationRecord extends ApplicationRecord {
    public static final String TYPE = RegionScopedApplicationRecord.class.getSimpleName();
    public static final String URI_SCHEME = "regionapp";

    public RegionScopedApplicationRecord() {
        super();
    }

    public RegionScopedApplicationRecord(String applicationName) {
        super(applicationName);
    }

    public RegionScopedApplicationRecord(String applicationName, long dataVersion) {
        super(applicationName, dataVersion);
    }

    public RegionScopedApplicationRecord(String applicationName, long dataVersion, int requiredActive) {
        super(applicationName, dataVersion, requiredActive);
    }

    public RegionScopedApplicationRecord(String applicationName, long dataVersion, List<String> resources) {
        super(applicationName, dataVersion, resources);
    }

    @Override
    public String getUrl() {
        return RegionScopedApplicationRecord.getUrl(getApplicationName());
    }

    public static String getUrl(String anAppName) {
        return String.format("%s:%s", RegionScopedApplicationRecord.URI_SCHEME, anAppName);
    }

    @Override
    public String getType() {
        return RegionScopedApplicationRecord.TYPE;
    }

    @Override
    public String getUriScheme() {
        return RegionScopedApplicationRecord.URI_SCHEME;
    }

}

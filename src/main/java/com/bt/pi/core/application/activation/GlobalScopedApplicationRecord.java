package com.bt.pi.core.application.activation;

import java.util.List;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.scope.NodeScope;

@Backupable
@EntityScope(scope = NodeScope.GLOBAL)
public class GlobalScopedApplicationRecord extends ApplicationRecord {
    public static final String TYPE = GlobalScopedApplicationRecord.class.getSimpleName();
    public static final String URI_SCHEME = "globalapp";

    public GlobalScopedApplicationRecord() {
        super();
    }

    public GlobalScopedApplicationRecord(String applicationName) {
        super(applicationName);
    }

    public GlobalScopedApplicationRecord(String applicationName, long dataVersion) {
        super(applicationName, dataVersion);
    }

    public GlobalScopedApplicationRecord(String applicationName, long dataVersion, int requiredActive) {
        super(applicationName, dataVersion, requiredActive);
    }

    public GlobalScopedApplicationRecord(String applicationName, long dataVersion, List<String> resources) {
        super(applicationName, dataVersion, resources);
    }

    @Override
    public String getUrl() {
        return GlobalScopedApplicationRecord.getUrl(getApplicationName());
    }

    public static String getUrl(String anAppName) {
        return String.format("%s:%s", GlobalScopedApplicationRecord.URI_SCHEME, anAppName);
    }

    @Override
    public String getType() {
        return GlobalScopedApplicationRecord.TYPE;
    }

    @Override
    public String getUriScheme() {
        return GlobalScopedApplicationRecord.URI_SCHEME;
    }
}

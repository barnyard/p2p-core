package com.bt.pi.core.application.activation;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ApplicationStatus {
    NOT_INITIALIZED("not_initialized"), ACTIVE("active"), CHECKING("checking"), PASSIVE("passive");

    private static final Map<String, ApplicationStatus> LOOKUP = new HashMap<String, ApplicationStatus>();

    static {
        for (ApplicationStatus keytype : EnumSet.allOf(ApplicationStatus.class))
            LOOKUP.put(keytype.getStatus(), keytype);
    }

    private String status;

    private ApplicationStatus(String aStatus) {
        status = aStatus;
    }

    public String getStatus() {
        return status;
    }

    public static ApplicationStatus get(String aStatus) {
        return LOOKUP.get(aStatus);
    }
}

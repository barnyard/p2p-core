package com.bt.pi.core.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum EntityMethod {

    GET("get"), UPDATE("update"), CREATE("create"), DELETE("delete");

    private static final Map<String, EntityMethod> LOOKUP = new HashMap<String, EntityMethod>();

    static {
        for (EntityMethod keytype : EnumSet.allOf(EntityMethod.class))
            LOOKUP.put(keytype.getMethod(), keytype);
    }

    private String method;

    private EntityMethod(String str) {
        method = str;
    }

    public String getMethod() {
        return method;
    }

    public static EntityMethod get(String aMethod) {
        return LOOKUP.get(aMethod);
    }
}

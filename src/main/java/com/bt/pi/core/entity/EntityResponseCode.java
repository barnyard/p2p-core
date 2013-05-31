package com.bt.pi.core.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum EntityResponseCode {
    OK(200), NOT_FOUND(404), FORBIDDEN(403), ERROR(500);

    private static final Map<Integer, EntityResponseCode> LOOKUP = new HashMap<Integer, EntityResponseCode>();

    static {
        for (EntityResponseCode keytype : EnumSet.allOf(EntityResponseCode.class))
            LOOKUP.put(keytype.getCode(), keytype);
    }

    private int code;

    private EntityResponseCode(int aCode) {
        code = aCode;
    }

    public int getCode() {
        return code;
    }

    public static EntityResponseCode get(int aCode) {
        return LOOKUP.get(aCode);
    }
}

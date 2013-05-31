package com.bt.pi.core.entity;

public class PersistablePiEntityMapping {
    private String type;
    private String scheme;
    private Integer typeCode;

    public PersistablePiEntityMapping(String aType, String aScheme, Integer aTypeCode) {
        this.type = aType;
        this.scheme = aScheme;
        this.typeCode = aTypeCode;
    }

    public PersistablePiEntityMapping() {
        this(null, null, null);
    }

    public void setType(String aType) {
        this.type = aType;
    }

    public void setScheme(String aScheme) {
        this.scheme = aScheme;
    }

    public void setTypeCode(Integer aTypeCode) {
        this.typeCode = aTypeCode;
    }

    public String getType() {
        return type;
    }

    public String getScheme() {
        return scheme;
    }

    public int getTypeCode() {
        return typeCode;
    }

    @Override
    public String toString() {
        return String.format("[Class: %s, scheme: %s, type code: %d]", type, scheme, typeCode);
    }
}

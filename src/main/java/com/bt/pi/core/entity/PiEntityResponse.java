package com.bt.pi.core.entity;

public class PiEntityResponse {
    private PiEntity entity;
    private EntityResponseCode entityResponseCode;

    public PiEntityResponse() {
    }

    public PiEntityResponse(EntityResponseCode respnCode, PiEntity anEntity) {
        entityResponseCode = respnCode;
        entity = anEntity;
    }

    public PiEntity getEntity() {
        return entity;
    }

    public void setEntity(PiEntity aEntity) {
        this.entity = aEntity;
    }

    public EntityResponseCode getEntityResponseCode() {
        return entityResponseCode;
    }

    public void setEntityResponseCode(EntityResponseCode aEntityResponseCode) {
        this.entityResponseCode = aEntityResponseCode;
    }

}

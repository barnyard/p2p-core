package com.bt.pi.core.entity;

import com.bt.pi.core.past.content.VersionedContent;

public interface PiEntity extends VersionedContent, Locatable {
    String TYPE_PARAM = "type";

    /**
     * This method is used by Pi to aid in serialize and deserialize PiEntities.
     * 
     * @return Type unique string representing the entity.
     */
    String getType();

    String getUriScheme();
}

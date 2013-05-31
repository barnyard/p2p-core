package com.bt.pi.core.application.health.entity;

import com.bt.pi.core.entity.PiEntityCollection;

public class LogMessageEntityCollection extends PiEntityCollection<LogMessageEntity> {
    public LogMessageEntityCollection() {
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }
}

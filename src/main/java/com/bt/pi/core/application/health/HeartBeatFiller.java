/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;

public interface HeartBeatFiller {
    HeartbeatEntity populate(HeartbeatEntity heartbeat);
}

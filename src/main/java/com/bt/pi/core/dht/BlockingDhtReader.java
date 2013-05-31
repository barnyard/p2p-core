package com.bt.pi.core.dht;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public interface BlockingDhtReader extends DhtClient {
    /**
     * Blocking get operation
     * 
     * @param id
     */
    PiEntity get(final PId id);

    /**
     * Blocking get operation that gets the nearest copy of the data. There are no guarantees on the correctness of the
     * data retrieved.
     * 
     * @param id
     */
    PiEntity getAny(final PId id);
}

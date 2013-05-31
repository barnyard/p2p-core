package com.bt.pi.core.application;

import rice.p2p.scribe.ScribeMultiClient;

import com.bt.pi.core.entity.PiLocation;
import com.bt.pi.core.id.PId;

/**
 * Interface that all applications wishing to use Scribe (pub-sub) must implements.
 */
public interface KoalaPastryScribeApplication extends ScribeMultiClient {

    @Deprecated
    void subscribe(PiLocation atopic, ScribeMultiClient listener);

    @Deprecated
    void unsubscribe(PiLocation atopic, ScribeMultiClient listener);

    void subscribe(PId topicId, ScribeMultiClient listener);

    void unsubscribe(PId topicId, ScribeMultiClient listener);
}
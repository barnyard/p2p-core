package com.bt.pi.core.past;

import java.util.SortedSet;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.p2p.past.Past;
import rice.p2p.past.messaging.PastMessage;
import rice.pastry.leafset.LeafSet;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

public interface KoalaDHTStorage extends Past {
    String UNCHECKED = "unchecked";

    /**
     * Returns the first data object found for this id.
     * 
     * @param id
     * @param continuation
     */
    <T extends PiEntity> void getAny(PId id, Continuation<T, Exception> continuation);

    /**
     * Returns the most recent version of the data stored for the id.
     * 
     * @param id
     * @param continuation
     */
    <T extends PiEntity> void get(PId id, Continuation<T, Exception> continuation);

    /**
     * This will insert the data overwriting anything that is there.
     * 
     * @param id
     * @param continuation
     */
    <T extends PiEntity> void put(PId id, T content, Continuation<Boolean[], Exception> continuation);

    /**
     * 
     * @param backupNumber
     * @param scope
     * @param pastContentId
     * @return
     */
    SortedSet<String> generateBackupIds(int numberOfBackups, NodeScope scope, PId pastContentId);

    // helper methods for helper classes.
    int getPastMessageUID();

    @SuppressWarnings(UNCHECKED)
    void sendPastMessage(Id backupId, PastMessage message, Continuation continuation);

    KoalaIdFactory getKoalaIdFactory();

    @SuppressWarnings(UNCHECKED)
    Continuation getMessageResponseContinuation(PastMessage message);

    Id getLocalNodeId();

    LeafSet getLeafSet();

    @SuppressWarnings(UNCHECKED)
    void readObjectFromStorage(Id id, Continuation continuation);

}

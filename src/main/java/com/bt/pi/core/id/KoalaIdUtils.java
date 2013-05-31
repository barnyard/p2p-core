package com.bt.pi.core.id;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Id.Distance;
import rice.pastry.NodeHandle;

import com.bt.pi.core.scope.NodeScope;

@Component
public class KoalaIdUtils {
    private static final int SIXTEEN = 16;
    private static final Log LOG = LogFactory.getLog(KoalaIdUtils.class);

    public KoalaIdUtils() {
    }

    public Id getNodeIdClosestToId(SortedSet<String> nodeIdSet, Id idToCheck) {
        return getNodeIdClosestToId(nodeIdSet, idToCheck, NodeScope.GLOBAL);
    }

    public Id getNodeIdClosestToId(SortedSet<String> nodeIdSet, Id idToCheck, NodeScope scope) {
        LOG.debug(String.format("getNodeIdClosestToId(%s, %s, %s)", nodeIdSet, idToCheck.toStringFull(), scope));

        if (nodeIdSet == null) {
            LOG.debug("Null node set, returning null");
            return null;
        }

        SortedSet<String> idSetToCheck = new TreeSet<String>();
        for (String nodeId : nodeIdSet) {
            if (areIdsWithinSameScope(idToCheck.toStringFull(), nodeId, scope))
                idSetToCheck.add(nodeId);
        }

        return getIdClosestFromIdsInScope(idSetToCheck, idToCheck);
    }

    private boolean areIdsWithinSameScope(String id1, String id2, NodeScope scope) {
        boolean result = false;

        int regionForId1 = PId.getRegionFromId(id1);
        int globalAvailabilityZoneCodeForId1 = PId.getGlobalAvailabilityZoneCodeFromId(id1);
        switch (scope) {
        case REGION:
            int regionForId2 = PId.getRegionFromId(id2);
            if (regionForId1 == regionForId2)
                result = true;
            break;
        case AVAILABILITY_ZONE:
            int globalAvailabilityZoneCodeForId2 = PId.getGlobalAvailabilityZoneCodeFromId(id2);
            if (globalAvailabilityZoneCodeForId1 == globalAvailabilityZoneCodeForId2)
                result = true;
            break;
        default:
            result = true;
            break;
        }

        LOG.debug(String.format("Id %s and id %s are %swithin scope %s", id1, id2, result ? "" : "not ", scope));
        return result;
    }

    private Id getIdClosestFromIdsInScope(SortedSet<String> nodeIdSet, Id idToCheck) {
        if (nodeIdSet == null || nodeIdSet.isEmpty()) {
            LOG.debug("Empty node set, returning null");
            return null;
        }

        String closestActiveNodeIdString = null;
        if (nodeIdSet.size() == 1) {
            closestActiveNodeIdString = nodeIdSet.first();
        } else {
            String idToCheckString = idToCheck.toStringFull();
            SortedSet<String> headSet = nodeIdSet.headSet(idToCheckString);
            SortedSet<String> tailSet = nodeIdSet.tailSet(idToCheckString);

            String activeNodeBelow = headSet.isEmpty() ? tailSet.last() : headSet.last();
            String activeNodeAbove = tailSet.isEmpty() ? headSet.first() : tailSet.first();

            Distance distanceBelow = idToCheck.distanceFromId(rice.pastry.Id.build(activeNodeBelow));
            Distance distanceAbove = idToCheck.distanceFromId(rice.pastry.Id.build(activeNodeAbove));
            if (distanceBelow.compareTo(distanceAbove) < 0)
                closestActiveNodeIdString = activeNodeBelow;
            else
                closestActiveNodeIdString = activeNodeAbove;
        }
        Id closestActiveNodeId = rice.pastry.Id.build(closestActiveNodeIdString);
        LOG.debug(String.format("Closest id to %s is %s", idToCheck.toStringFull(), closestActiveNodeId.toStringFull()));
        return closestActiveNodeId;
    }

    /**
     * Index
     * 
     * @param myId
     * @param leafNodeHandles
     * @param idToCheck
     * @return index in the array ordered by distance from the idToCheck.
     */
    public static int getPositionFromId(Id myId, Collection<NodeHandle> leafNodeHandles, Id idToCheck) {
        SortedSet<IdPositionPair> idPositions = new TreeSet<IdPositionPair>();

        Distance distance = myId.distanceFromId(idToCheck);
        IdPositionPair myPair = new IdPositionPair(myId, distance);
        for (NodeHandle handle : leafNodeHandles) {
            idPositions.add(new IdPositionPair(handle.getId(), handle.getId().distanceFromId(idToCheck)));
        }
        return idPositions.headSet(myPair).size();
    }

    public boolean isIdClosestToMe(String myId, Collection<NodeHandle> leafNodeHandles, Id idToCheck) {
        return isIdClosestToMe(myId, leafNodeHandles, idToCheck, NodeScope.GLOBAL);
    }

    public boolean isIdClosestToMe(String myId, Collection<NodeHandle> leafNodeHandles, Id idToCheck, NodeScope nodeScope) {
        LOG.debug(String.format("isIdClosestToMe(%s, %s, %s, %s)", myId, leafNodeHandles, idToCheck, nodeScope));
        SortedSet<String> nodeIds = new TreeSet<String>();
        nodeIds.add(myId);
        for (NodeHandle nodeHandle : leafNodeHandles) {
            nodeIds.add(nodeHandle.getId().toStringFull());
        }
        Id nodeIdClosestToId = getNodeIdClosestToId(nodeIds, idToCheck, nodeScope);
        return nodeIdClosestToId.toStringFull().equals(myId);
    }

    public static boolean isBackupId(Id idToCheck) {
        if (null == idToCheck)
            return false;
        LOG.debug(String.format("isBackupId(%s)", idToCheck.toStringFull()));
        String idStr = idToCheck.toStringFull();
        String lastCharacter = idStr.substring(idStr.length() - 1);
        int i = Integer.parseInt(lastCharacter, SIXTEEN);
        return i % 2 != 0;
    }
}

class IdPositionPair implements Comparable<IdPositionPair> {

    private Id id;
    private Distance distance;

    public IdPositionPair(Id anId, Distance idDistance) {
        id = anId;
        distance = idDistance;
    }

    public Id getId() {
        return id;
    }

    public Distance getDistance() {
        return distance;
    }

    @Override
    public int compareTo(IdPositionPair arg0) {
        return this.getDistance().compareTo(arg0.getDistance());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IdPositionPair castOther = (IdPositionPair) obj;
        return new EqualsBuilder().appendSuper(super.equals(obj)).append(distance, castOther.distance).append(id, castOther.id).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(distance).append(id).toHashCode();
    }

}
package com.bt.pi.core.application.activation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.conf.IllegalAnnotationException;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.KoalaIdUtils;

public abstract class ApplicationRecord extends PiEntityBase {
    private static final int MILLIS_IN_A_SECOND = 1000;
    private static final String TRIED_TO_TIMESTAMP_ACTIVE_NODE_S_BUT_NODE_WAS_NOT_FOUND_TO_BE_ACTIVE = "Tried to timestamp active node %s, but node was not found to be active!";
    private static final Log LOG = LogFactory.getLog(ApplicationRecord.class);
    private String appName;

    private Map<String, TimeStampedPair<String>> resourceToNodeMap = new TreeMap<String, TimeStampedPair<String>>();

    public ApplicationRecord() {
        this(null);
    }

    public ApplicationRecord(String applicationName) {
        this(applicationName, -1);
    }

    public ApplicationRecord(String applicationName, long dataVersion) {
        this(applicationName, dataVersion, 1);
    }

    public ApplicationRecord(String applicationName, long dataVersion, int requiredActive) {
        this(applicationName, dataVersion, null);
        List<String> resources = new ArrayList<String>();
        for (int i = requiredActive; i > 0; i--) {
            resources.add(String.valueOf(i));
        }
        addResources(resources);
    }

    public ApplicationRecord(String applicationName, long dataVersion, List<String> resources) {
        setApplicationName(applicationName);
        setVersion(dataVersion);
        addResources(resources);
    }

    public void addResources(List<String> resources) {
        if (resources != null && resources.size() > 0) {
            for (String resource : resources) {
                resourceToNodeMap.put(resource, null);
            }
        }
    }

    public void removeResources(List<String> resources) {
        for (String resource : resources) {
            resourceToNodeMap.remove(resource);
        }
    }

    public String getApplicationName() {
        return appName;
    }

    public void setApplicationName(String applicationName) {
        this.appName = applicationName;
    }

    public int getRequiredActive() {
        return resourceToNodeMap.size();
    }

    public boolean addCurrentlyActiveNode(Id nodeId, long expirySeconds) {
        LOG.debug(String.format("addCurrentlyActiveNode(%s, %d)", nodeId.toStringFull(), expirySeconds));
        Collection<TimeStampedPair<String>> values = getActiveNodeMap().values();
        // if space and we are not listed.
        TimeStampedPair<String> timeStampedNodeId = newTimeStampedPair(nodeId);
        if (values.contains(timeStampedNodeId)) {
            LOG.debug(String.format("Node %s has already been added as active, timestamping it", nodeId.toStringFull()));
            return timestampActiveNode(nodeId);
        }

        String resourceKey = getFirstFreeResource();
        if (null != resourceKey) {
            LOG.debug(String.format("first free resource: %s", resourceKey));
            resourceToNodeMap.put(resourceKey, timeStampedNodeId);
            return true;
        }

        if (expirySeconds < 1)
            return false;

        String expiredResourceKey = getFirstExpiredResource(expirySeconds);
        if (null != expiredResourceKey) {
            LOG.debug(String.format("first expired resource: %s", expiredResourceKey));
            resourceToNodeMap.put(expiredResourceKey, timeStampedNodeId);
            return true;
        }

        return false;
    }

    private String getFirstExpiredResource(long expirySeconds) {
        for (Entry<String, TimeStampedPair<String>> entry : getActiveNodeMap().entrySet()) {
            TimeStampedPair<String> timeStampedNodeId = entry.getValue();
            if (expired(timeStampedNodeId, expirySeconds))
                return entry.getKey();
        }
        return null;
    }

    private boolean expired(TimeStampedPair<String> timeStampedPair, long expirySeconds) {
        long timeStamp = timeStampedPair.getTimeStamp();
        return (timeStamp + (expirySeconds * MILLIS_IN_A_SECOND)) < System.currentTimeMillis();
    }

    public boolean timestampActiveNode(Id nodeId) {
        LOG.debug(String.format("timestampActiveNode(%s)", nodeId.toStringFull()));
        String associatedResource = getAssociatedResource(nodeId);
        LOG.debug("associatedResource: " + associatedResource);
        if (null == associatedResource) {
            LOG.warn(String.format(TRIED_TO_TIMESTAMP_ACTIVE_NODE_S_BUT_NODE_WAS_NOT_FOUND_TO_BE_ACTIVE, nodeId.toStringFull()));
            return false;
        }
        TimeStampedPair<String> existingTimestampedNodeId = getActiveNodeMap().get(associatedResource);
        if (existingTimestampedNodeId == null) {
            LOG.warn(String.format(TRIED_TO_TIMESTAMP_ACTIVE_NODE_S_BUT_NODE_WAS_NOT_FOUND_TO_BE_ACTIVE, nodeId.toStringFull()));
            return false;
        }
        existingTimestampedNodeId.updateTimeStamp();
        return true;
    }

    protected TimeStampedPair<String> newTimeStampedPair(Id nodeId) {
        return new TimeStampedPair<String>(nodeId.toStringFull());
    }

    private String getFirstFreeResource() {
        for (Entry<String, TimeStampedPair<String>> entry : getActiveNodeMap().entrySet()) {
            TimeStampedPair<String> timeStampedNodeId = entry.getValue();
            if (null == timeStampedNodeId)
                return entry.getKey();
        }
        return null;
    }

    private String getResourceForNodeId(String value) {
        for (Entry<String, TimeStampedPair<String>> entry : getActiveNodeMap().entrySet()) {
            TimeStampedPair<String> timeStampedNodeId = entry.getValue();
            if (null == timeStampedNodeId)
                continue;
            if (value.equalsIgnoreCase(timeStampedNodeId.getObject()))
                return entry.getKey();
        }
        return null;
    }

    public String getAssociatedResource(Id nodeId) {
        return getResourceForNodeId(nodeId.toStringFull());
    }

    public boolean containsNodeId(Id nodId) {
        return getResourceForNodeId(nodId.toStringFull()) != null;
    }

    public boolean removeActiveNode(Id nodeId) {
        String nodeIdFullString = nodeId.toStringFull();
        return removeActiveNode(nodeIdFullString);
    }

    public boolean removeActiveNode(String nodeIdFullString) {
        String key = getResourceForNodeId(nodeIdFullString);
        if (key != null) {
            boolean result = this.resourceToNodeMap.containsKey(key);
            this.resourceToNodeMap.put(key, null);
            return result;
        }
        return false;
    }

    public Map<String, TimeStampedPair<String>> getActiveNodeMap() {
        return resourceToNodeMap;
    }

    public void setActiveNodeMap(SortedMap<String, TimeStampedPair<String>> aCurrentlyActiveNodeList) {
        resourceToNodeMap = aCurrentlyActiveNodeList;
    }

    public void setCurrentlyActiveNodeMap(SortedMap<String, String> aCurrentlyActiveNodeList) {
        resourceToNodeMap = new HashMap<String, TimeStampedPair<String>>();
        for (Entry<String, String> entry : aCurrentlyActiveNodeList.entrySet()) {
            if (null == entry.getValue()) {
                resourceToNodeMap.put(entry.getKey(), null);
            } else {
                resourceToNodeMap.put(entry.getKey(), new TimeStampedPair<String>(entry.getValue()));
            }
        }
    }

    private SortedSet<String> createSortedOfActiveNodes() {
        SortedSet<String> result = new TreeSet<String>();
        for (TimeStampedPair<String> pair : resourceToNodeMap.values())
            if (null != pair)
                if (null != pair.getObject())
                    result.add(pair.getObject());
        return result;
    }

    public int getNumCurrentlyActiveNodes() {
        return createSortedOfActiveNodes().size();
    }

    public Id getClosestActiveNodeId(Id refNodeId) {
        LOG.debug(String.format("Getting id closest to %s from set %s", refNodeId, resourceToNodeMap));
        SortedSet<String> activeNodeSet = createSortedOfActiveNodes();

        EntityScope entityScope = getClass().getAnnotation(EntityScope.class);
        if (entityScope == null)
            throw new IllegalAnnotationException("Application records should contain the @EntityScope annotation");

        KoalaIdUtils koalaIdUtils = new KoalaIdUtils();
        return koalaIdUtils.getNodeIdClosestToId(activeNodeSet, refNodeId, entityScope.scope());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("appName", appName).append("version", getVersion()).append("currentlyActiveNodeList", resourceToNodeMap).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ApplicationRecord))
            return false;
        ApplicationRecord castOther = (ApplicationRecord) other;
        return new EqualsBuilder().append(appName, castOther.appName).append(getVersion(), castOther.getVersion()).append(resourceToNodeMap, castOther.resourceToNodeMap).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(appName).append(getVersion()).append(resourceToNodeMap).toHashCode();
    }
}

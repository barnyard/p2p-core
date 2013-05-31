/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health.entity;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.bt.pi.core.application.reporter.ReportableEntity;

public class HeartbeatEntity extends ReportableEntity<HeartbeatEntity> {
    private static final Log LOG = LogFactory.getLog(HeartbeatEntity.class);

    private Date timestamp;
    private String theHostname;
    private Map<String, Long> diskspace;
    private Map<String, Long> memoryDetails;
    private Collection<String> leafset;
    private Collection<LogMessageEntity> ipmiEvents;
    private Map<String, Long> availableResources;

    public HeartbeatEntity(String hostname) {
        super(hostname);
        diskspace = new HashMap<String, Long>();
        memoryDetails = new HashMap<String, Long>();
        timestamp = new Date(System.currentTimeMillis());
        leafset = new ArrayList<String>();
        ipmiEvents = new ArrayList<LogMessageEntity>();
        availableResources = new HashMap<String, Long>();
        setHostname(hostname);
    }

    public HeartbeatEntity() {
        this(null);
    }

    @Override
    public String getType() {
        return this.getClass().getSimpleName();
    }

    @JsonIgnore
    @Override
    public String getUrl() {
        return null;
    }

    public String getTimestamp() {
        return DateFormat.getDateTimeInstance().format(timestamp);
    }

    public void setTimestamp(String aTimestamp) {
        try {
            this.timestamp = DateFormat.getDateTimeInstance().parse(aTimestamp);
        } catch (ParseException e) {
            LOG.error(String.format("Unable to parse date: %s", aTimestamp), e);
        }
    }

    public String getHostname() {
        return theHostname;
    }

    public void setHostname(String hostname) {
        theHostname = hostname;
    }

    public Map<String, Long> getDiskSpace() {
        return diskspace;
    }

    public void setDiskSpace(Map<String, Long> theDiskSpace) {
        diskspace = theDiskSpace;
    }

    public Map<String, Long> getMemoryDetails() {
        return memoryDetails;
    }

    public void setMemoryDetails(Map<String, Long> theMemoryDetails) {
        memoryDetails = theMemoryDetails;
    }

    public void setLeafSet(Collection<String> theLeafset) {
        leafset = theLeafset;
    }

    public Collection<String> getLeafSet() {
        return leafset;
    }

    public void setIPMIEvents(Collection<LogMessageEntity> events) {
        ipmiEvents = events;
    }

    public Collection<LogMessageEntity> getIPMIEvents() {
        return ipmiEvents;
    }

    public Map<String, Long> getAvailableResources() {
        return availableResources;
    }

    public void setAvailableResources(Map<String, Long> theAvailableResources) {
        this.availableResources = theAvailableResources;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HeartbeatEntity:{");
        sb.append("nodeId:").append(getNodeId());
        sb.append(", Timestamp:").append(getTimestamp());
        sb.append(", hostname:").append(getHostname());
        sb.append(", diskspace:").append(getDiskSpace());
        sb.append(", Leafset:").append(leafset);
        sb.append(", ipmievents:").append(ipmiEvents);
        sb.append(", availableResources:").append(availableResources);
        sb.append("}");
        return sb.toString();

    }

    @Override
    public int compareTo(HeartbeatEntity other) {
        int result = timestamp.compareTo(other.timestamp);
        if (result != 0)
            return result;

        if (!new EqualsBuilder().append(getHostname(), other.getHostname()).append(getDiskSpace(), other.getDiskSpace()).append(getLeafSet(), other.getLeafSet()).append(getMemoryDetails(), other.getMemoryDetails()).append(getNodeId(),
                other.getNodeId()).append(getIPMIEvents(), other.getIPMIEvents()).isEquals())
            return 1;

        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (null == other)
            return false;
        if (!(other instanceof HeartbeatEntity))
            return false;
        HeartbeatEntity hb = (HeartbeatEntity) other;
        return new EqualsBuilder().append(getTimestamp(), hb.getTimestamp()).append(getHostname(), hb.getHostname()).append(getDiskSpace(), hb.getDiskSpace()).append(getLeafSet(), hb.getLeafSet()).append(getMemoryDetails(), hb.getMemoryDetails())
                .append(getNodeId(), hb.getNodeId()).append(getIPMIEvents(), hb.getIPMIEvents()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getTimestamp()).append(getDiskSpace()).append(getHostname()).append(getLeafSet()).append(getMemoryDetails()).append(getNodeId()).append(getIPMIEvents()).toHashCode();
    }

    @JsonIgnore
    @Override
    public int getKeysForMapCount() {
        return 1;
    }

    @JsonIgnore
    @Override
    public Object[] getKeysForMap() {
        return new String[] { getNodeId() };
    }

    @Override
    public long getCreationTime() {
        return timestamp.getTime();
    }

    @Override
    public String getUriScheme() {
        return this.getClass().getSimpleName();
    }
}

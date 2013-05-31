/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ HeartbeatEntity.class })
public class HeartbeatEntityTest {
    private static final String NODE_ID = "nodeId";
    private static final long CURRENT_TIME = 10000l;
    private static final String HOSTNAME = "hostname";
    private HeartbeatEntity heartbeat;
    private Map<String, Long> diskSpace;
    private Collection<String> leafset;
    private Collection<LogMessageEntity> ipmiEvents;

    @SuppressWarnings("unchecked")
    @Before
    public void doBefore() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis()).thenReturn(CURRENT_TIME);
        heartbeat = new HeartbeatEntity(NODE_ID);

        diskSpace = Mockito.mock(Map.class);
        leafset = Mockito.mock(Collection.class);

        ipmiEvents = mock(Collection.class);
    }

    @Test
    public void getUrlShouldBeNull() {
        assertNull(heartbeat.getUrl());
    }

    @Test
    public void getTypeShouldBeHeartbeat() {
        assertEquals("HeartbeatEntity", heartbeat.getType());
    }

    @Test
    public void getTimestampShouldBeWhenCreated() throws Exception {
        assertEquals(DateFormat.getDateTimeInstance().format(new Date(CURRENT_TIME)), heartbeat.getTimestamp());
    }

    @Test
    public void mapsAndCollectionsShouldBeConstructed() {
        assertNotNull(heartbeat.getDiskSpace());
        assertNotNull(heartbeat.getMemoryDetails());
        assertNotNull(heartbeat.getLeafSet());
        assertNotNull(heartbeat.getIPMIEvents());
    }

    @Test
    public void gettersAndSettersShouldBeSymmetrical() {
        heartbeat.setHostname(HOSTNAME);
        heartbeat.setDiskSpace(diskSpace);
        heartbeat.setMemoryDetails(diskSpace);
        heartbeat.setLeafSet(leafset);
        heartbeat.setIPMIEvents(ipmiEvents);

        assertEquals(HOSTNAME, heartbeat.getHostname());
        assertEquals(diskSpace, heartbeat.getDiskSpace());
        assertEquals(diskSpace, heartbeat.getMemoryDetails());
        assertEquals(leafset, heartbeat.getLeafSet());
        assertEquals(ipmiEvents, heartbeat.getIPMIEvents());
    }

    @Test
    public void comparingWithEarlierTimestampShouldReturnPositive() {
        PowerMockito.when(System.currentTimeMillis()).thenReturn(CURRENT_TIME - 50);

        final HeartbeatEntity other = copyHeartbeat();

        assertTrue(heartbeat.compareTo(other) > 0);
    }

    private HeartbeatEntity copyHeartbeat() {
        final HeartbeatEntity other = new HeartbeatEntity(heartbeat.getNodeId());
        other.setDiskSpace(heartbeat.getDiskSpace());
        other.setLeafSet(heartbeat.getLeafSet());
        other.setHostname(heartbeat.getHostname());
        other.setMemoryDetails(heartbeat.getMemoryDetails());

        return other;
    }

    @Test
    public void comparingWithLaterTimestampShouldReturnNegative() {
        PowerMockito.when(System.currentTimeMillis()).thenReturn(CURRENT_TIME + 50);

        final HeartbeatEntity other = copyHeartbeat();

        assertTrue(heartbeat.compareTo(other) < 0);
    }

    @Test
    public void comparingWithEqualTimestampShouldReturn0() {
        PowerMockito.when(System.currentTimeMillis()).thenReturn(CURRENT_TIME);
        assertEquals(0, heartbeat.compareTo(new HeartbeatEntity(NODE_ID)));
    }

    @Test
    public void comparingWithEqualTimestampButDifferentOtherFieldsShouldNotReturn0() {
        heartbeat.setHostname(HOSTNAME);
        assertFalse(0 == heartbeat.compareTo(new HeartbeatEntity("")));
    }

    @Test
    public void itShouldNotBeEqualToNull() {
        assertFalse(heartbeat.equals(null));
    }

    @Test
    public void itShouldNotBeEqualToDifferentTypes() {
        assertFalse(heartbeat.equals(mock(Object.class)));
    }

    @Test
    public void itShouldBeEqualToItself() {
        assertTrue(heartbeat.equals(heartbeat));
    }

    @Test
    public void itShouldBeEqualWhenFieldsAreEqual() {
        heartbeat.setHostname(HOSTNAME);

        HeartbeatEntity other = new HeartbeatEntity(NODE_ID);
        other.setHostname(HOSTNAME);

        assertTrue(heartbeat.equals(other));
    }

    @Test
    public void itShouldHaveAHashcode() {
        assertNotNull(heartbeat.hashCode());
    }
}

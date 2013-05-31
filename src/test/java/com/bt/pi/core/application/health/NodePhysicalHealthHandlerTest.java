/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.reporter.NodeBasedReportableEntityStore;
import com.bt.pi.core.entity.PiEntityCollection;

@RunWith(MockitoJUnitRunner.class)
public class NodePhysicalHealthHandlerTest {
    private static final String NODE_ID = "nodeId";
    @InjectMocks
    private NodePhysicalHealthHandler handler = new NodePhysicalHealthHandler();
    @Mock
    NodeBasedReportableEntityStore<HeartbeatEntity> reportableEntityStore;
    @Mock
    private PiEntityCollection<HeartbeatEntity> heartbeatCollection;
    private Collection<HeartbeatEntity> heartbeats;
    @Mock
    private HeartbeatEntity heartbeat;

    @Before
    public void doBefore() {

        heartbeats = Arrays.asList(heartbeat);

        when(heartbeat.getKeysForMap()).thenReturn(new Object[] { NODE_ID });
        when(heartbeat.getKeysForMapCount()).thenReturn(1);
        when(heartbeat.getNodeId()).thenReturn(NODE_ID);
        when(heartbeatCollection.getEntities()).thenReturn(heartbeats);
    }

    @Test
    public void getEntitiesByNodeIdShouldDoThat() {
        // setup
        when(reportableEntityStore.getByNodeId(NODE_ID)).thenReturn(heartbeat);
        // act
        handler.receive(heartbeatCollection, true);
        // assert
        assertNotNull(handler.getEntityByNodeId(NODE_ID));
        assertEquals(heartbeat, handler.getEntityByNodeId(NODE_ID));
    }

    @Test
    public void shouldReturnNotNullPiEntityCollection() {
        assertNotNull(handler.getPiEntityCollection());
    }

    @Test
    public void shouldSetBroadcastWindowSize() {
        // act
        handler.setBroadcastWindowSize(100);
        // assert
        int broadcastWindowSize = (Integer) ReflectionTestUtils.getField(handler, "broadcastWindowSize");
        assertEquals(100, broadcastWindowSize);
    }

    @Test
    public void shouldSetPublishIntervalSeconds() {
        // act
        handler.setPublishIntervalSeconds(100);
        // assert
        assertEquals(new Long(100), (Long) ReflectionTestUtils.getField(handler, "publishIntervalSeconds"));
    }

    @Test
    public void testReceiveHeartbeat() {
        // act
        handler.receive(heartbeat);
        // verify
        verify(reportableEntityStore).add(heartbeat);

    }
}

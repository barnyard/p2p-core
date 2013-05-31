package com.bt.pi.core.application.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.entity.PiEntityCollection;

@SuppressWarnings("unchecked")
public class LogMessageHandlerTest {
    private static final String UNWANTED_NODE_ID = "unwantedNodeId";
    private static final String NODE_ID = "nodeId";

    private LogMessageHandler handler;
    private PiEntityCollection<LogMessageEntity> logMessageEntities;
    private Collection<LogMessageEntity> entities;

    @Before
    public void doBefore() {
        entities = new ArrayList<LogMessageEntity>();
        entities.add(new LogMessageEntity(123, "log message", "test class", "tx id", NODE_ID));

        logMessageEntities = mock(PiEntityCollection.class);
        when(logMessageEntities.getEntities()).thenReturn(entities);

        handler = new LogMessageHandler();
        handler.setKeepCount(100);
        handler.setBroadcastWindowSize(10);
    }

    @Test
    public void receivedLogMessagesShouldBeRetrievableByNodeId() {
        // act
        handler.receive(logMessageEntities, true);

        // assert
        assertContainsSameLogMessages(entities, handler.getEntitiesByNodeId(NODE_ID));
    }

    @Test
    public void receivedLogMessagesFromTheSameNodeShouldBeRetrievableByNodeId() {
        // setup
        Collection<LogMessageEntity> node1Entities = new ArrayList<LogMessageEntity>();
        Collection<LogMessageEntity> node2Entities = new ArrayList<LogMessageEntity>();
        List<LogMessageEntity> keepers = new ArrayList<LogMessageEntity>();
        addEvents(keepers, 0, 5);
        node1Entities.addAll(keepers.subList(0, 2));
        node2Entities.addAll(keepers.subList(2, 5));

        when(logMessageEntities.getEntities()).thenReturn((Collection<LogMessageEntity>) node1Entities);
        handler.receive(logMessageEntities, false);

        when(logMessageEntities.getEntities()).thenReturn((Collection<LogMessageEntity>) node2Entities);
        handler.receive(logMessageEntities, false);

        // assert
        assertContainsSameLogMessages(keepers, handler.getEntitiesByNodeId(NODE_ID));
    }

    @Test
    public void onlyLastNMessagesShouldBeKeptInMemory() {
        // setup
        List<LogMessageEntity> keepers = sendTooManyEntities();

        // assert
        assertContainsSameLogMessages(keepers, handler.getAllEntities().getEntities());
    }

    @Test
    public void onlyLastNMessagesShouldBeGettableByNodeId() {
        // setup
        List<LogMessageEntity> keepers = sendTooManyEntities();

        // assert
        assertContainsSameLogMessages(keepers.subList(0, 2), handler.getEntitiesByNodeId(NODE_ID));
    }

    @Test
    public void onlyNodeIdsWithMessagesShouldBeGettable() {
        // setup
        sendTooManyEntities();

        // assert
        assertNull(handler.getEntitiesByNodeId(UNWANTED_NODE_ID));
    }

    @Test
    public void shouldGetAllEntitiesInReverseOrder() {
        // setup
        sendTooManyEntities();

        // act
        PiEntityCollection<LogMessageEntity> result = handler.getAllEntities();

        // assert
        assertEquals(10, result.getEntities().size());
        Iterator<LogMessageEntity> iterator = result.getEntities().iterator();
        long last = Long.MAX_VALUE;
        while (iterator.hasNext()) {
            long creationTime = iterator.next().getCreationTime();
            assertTrue(creationTime <= last);
            last = creationTime;
        }
    }

    private List<LogMessageEntity> sendTooManyEntities() {
        String nodeId1 = NODE_ID;
        String nodeId2 = "2";
        String nodeId3 = "3";
        String nodeId4 = "4";

        List<LogMessageEntity> keepers = new ArrayList<LogMessageEntity>();
        List<LogMessageEntity> unwanted = new ArrayList<LogMessageEntity>();
        addEvents(unwanted, 0, 2, nodeId1);
        addEvents(unwanted, 2, 5, nodeId2);
        addEvents(unwanted, 5, 8, nodeId3);
        addEvents(unwanted, 8, 10, nodeId4);

        addEvents(keepers, 10, 12, nodeId1);
        addEvents(keepers, 12, 15, nodeId2);
        addEvents(keepers, 15, 20, nodeId3);

        final int keepCount = 10;
        handler.setKeepCount(keepCount);

        Collection<LogMessageEntity> node1Entities = new ArrayList<LogMessageEntity>();
        Collection<LogMessageEntity> node2Entities = new ArrayList<LogMessageEntity>();
        Collection<LogMessageEntity> node3Entities = new ArrayList<LogMessageEntity>();
        Collection<LogMessageEntity> node4Entities = new ArrayList<LogMessageEntity>();

        node1Entities.addAll(unwanted.subList(0, 2));
        node2Entities.addAll(unwanted.subList(2, 5));
        node3Entities.addAll(unwanted.subList(5, 8));
        node4Entities.addAll(unwanted.subList(8, 10));
        node1Entities.addAll(keepers.subList(0, 2));
        node2Entities.addAll(keepers.subList(2, 5));
        node3Entities.addAll(keepers.subList(5, 10));

        when(logMessageEntities.getEntities()).thenReturn(node1Entities);
        handler.receiveFromNode(logMessageEntities);

        when(logMessageEntities.getEntities()).thenReturn(node2Entities);
        handler.receiveFromNode(logMessageEntities);

        when(logMessageEntities.getEntities()).thenReturn(node3Entities);
        handler.receiveFromNode(logMessageEntities);

        when(logMessageEntities.getEntities()).thenReturn(node4Entities);
        handler.receiveFromNode(logMessageEntities);
        return keepers;
    }

    private void addEvents(Collection<LogMessageEntity> theEntities, int start, int end) {
        addEvents(theEntities, start, end, NODE_ID);
    }

    private void addEvents(Collection<LogMessageEntity> theEntities, int start, int end, String nodeId) {
        for (int i = start; i < end; i++) {
            theEntities.add(buildEvent(i * 1000l, nodeId));
        }
    }

    private void assertContainsSameLogMessages(Collection<LogMessageEntity> expected, Collection<LogMessageEntity> actual) {
        assertEquals(expected.size(), actual.size());
        for (LogMessageEntity logMessageEntity : actual) {
            assertTrue(logMessageEntity.toString(), expected.contains(logMessageEntity));
        }
    }

    private LogMessageEntity buildEvent(long timestamp, String nodeId) {
        return new LogMessageEntity(timestamp, "log message", "test class", "tx id" + timestamp, nodeId);
    }
}

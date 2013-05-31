package com.bt.pi.core.application.health.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.text.DateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class LogMessageEntityTest {
    private long timestamp = 1234;
    private Date date = new Date(timestamp);
    private String logMessage = "log message";
    private String className = "test class";
    private String logTxId = "tx id";
    private String nodeId = "nodeId";

    private LogMessageEntity logMessageEntity;

    @Before
    public void setup() {
        logMessageEntity = new LogMessageEntity(null);
    }

    @Test
    public void testConstructor() {
        // act
        logMessageEntity = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);

        // assert
        assertEquals(DateFormat.getDateTimeInstance().format(date), logMessageEntity.getTimestamp());
        assertEquals(logMessage, logMessageEntity.getLogMessage());
        assertEquals(className, logMessageEntity.getClassName());
        assertEquals(logTxId, logMessageEntity.getLogTxId());
        assertEquals(nodeId, logMessageEntity.getNodeId());
    }

    @Test
    public void testGettersAndSetters() {
        // setup
        String formattedTime = DateFormat.getDateTimeInstance().format(date);
        logMessageEntity = new LogMessageEntity(nodeId);

        // act
        logMessageEntity.setTimestamp(formattedTime);
        logMessageEntity.setLogMessage(logMessage);
        logMessageEntity.setClassName(className);
        logMessageEntity.setLogTxId(logTxId);

        // assert
        assertEquals(formattedTime, logMessageEntity.getTimestamp());
        assertEquals(logMessage, logMessageEntity.getLogMessage());
        assertEquals(className, logMessageEntity.getClassName());
        assertEquals(logTxId, logMessageEntity.getLogTxId());
        assertEquals(nodeId, logMessageEntity.getNodeId());
    }

    @Test
    public void testTwoEntitiesWithSameTimestampAndSameLogMessageAreEqual() throws Exception {
        // setup
        logMessageEntity = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);
        LogMessageEntity logMessageEntity2 = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);

        // act
        int result = logMessageEntity.compareTo(logMessageEntity2);

        // assert
        assertEquals(0, result);
    }

    @Test
    public void testTwoEntitiesWithSameTimestampAndDifferentLogMessagesComparesOnLogMessages() throws Exception {
        // setup
        logMessageEntity = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);
        LogMessageEntity logMessageEntity2 = new LogMessageEntity(timestamp, logMessage + "2", className, logTxId, nodeId);

        // act
        int result = logMessageEntity.compareTo(logMessageEntity2);

        // assert
        assertTrue(String.valueOf(result), result < 0);
    }

    @Test
    public void testTwoEntitiesWithDifferentTimestampDoesNotLookAtLogMessage() throws Exception {
        // setup
        logMessageEntity = new LogMessageEntity(timestamp + 100000, logMessage, className, logTxId, nodeId);
        LogMessageEntity logMessageEntity2 = new LogMessageEntity(timestamp, logMessage + "2", className, logTxId, nodeId);

        // act
        int result = logMessageEntity.compareTo(logMessageEntity2);

        // assert
        assertTrue(String.valueOf(result), result > 0);
    }

    @Test
    public void itShouldNotBeEqualToNull() {
        assertFalse(logMessageEntity.equals(null));
    }

    @Test
    public void itShouldNotBeEqualToDifferentTypes() {
        assertFalse(logMessageEntity.equals(mock(Object.class)));

    }

    @Test
    public void testEquals() {
        // setup
        logMessageEntity = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);
        LogMessageEntity logMessageEntity2 = new LogMessageEntity(timestamp, logMessage, className, logTxId, nodeId);

        // assert
        assertEquals(logMessageEntity, logMessageEntity2);
    }

    @Test
    public void getUrlShouldReturnNull() {
        assertNull(logMessageEntity.getUrl());
    }

    @Test
    public void getTypeShouldReturnLogMessageEntity() {
        assertEquals("LogMessageEntity", logMessageEntity.getType());
    }
}

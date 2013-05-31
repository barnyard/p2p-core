package com.bt.pi.core.application.health.entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class LogMessageEntityCollectionTest {
    private LogMessageEntityCollection logMessageEntityCollection = new LogMessageEntityCollection();

    @Test
    public void shouldGetType() throws Exception {
        // act
        String result = logMessageEntityCollection.getType();

        // assert
        assertThat(result, equalTo("LogMessageEntityCollection"));
    }

    @Test
    public void shouldBeAbleToDoToStringForLargeRecordInReasonableTime() {
        // setup
        Log LOG = LogFactory.getLog(LogMessageEntityCollection.class);

        Collection<LogMessageEntity> entities = new ArrayList<LogMessageEntity>();
        for (int i = 0; i < 10000; i++)
            entities.add(new LogMessageEntity(System.currentTimeMillis(), "test", getClass().getName(), null, null));
        logMessageEntityCollection.setEntities(entities);

        // act
        long timestampBefore = System.currentTimeMillis();
        String res = logMessageEntityCollection.toString();
        LOG.info(res);
        long timestampAfter = System.currentTimeMillis();

        // assert
        assertTrue(timestampAfter - timestampBefore < 1000);
    }

}

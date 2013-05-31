package com.bt.pi.core.scribe;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.scribe.content.KoalaScribeContent;

public class AnycastFailureEventTest {
    private AnycastFailureEvent anycastFailureEvent;
    private Id id;
    private KoalaScribeContent content;

    @Test
    public void testGetters() throws Exception {
        // setup
        id = mock(Id.class);
        content = mock(KoalaScribeContent.class);

        // act
        anycastFailureEvent = new AnycastFailureEvent(this, id, content);

        // assert
        assertEquals(this, anycastFailureEvent.getSource());
        assertEquals(id, anycastFailureEvent.getTopicId());
        assertEquals(content, anycastFailureEvent.getKoalaScribeContent());
    }
}

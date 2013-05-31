package com.bt.pi.core.past.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import rice.p2p.commonapi.Id;
import rice.p2p.past.gc.GCPastMetadata;

import com.bt.pi.core.scope.NodeScope;

public class KoalaPiEntityContentTest {
    private Id id = mock(Id.class);
    private String entity = "loser";
    private KoalaPiEntityContent koalaPiEntityContent = new KoalaPiEntityContent(id, entity, true, "test", 3, NodeScope.AVAILABILITY_ZONE, "url", 34);

    @Test
    public void testDuplicate() {
        // setup

        // act
        KoalaPiEntityContent result = koalaPiEntityContent.duplicate();

        // assert
        assertTrue(result instanceof KoalaPiEntityContent);
        assertEquals(koalaPiEntityContent, result);
    }

    @Test
    public void testGetMetadata() {
        // act
        GCPastMetadata result = this.koalaPiEntityContent.getMetadata(2);

        // assert
        assertTrue(result instanceof KoalaGCPastMetadata);
        KoalaGCPastMetadata koalaGCPastMetadata = (KoalaGCPastMetadata) result;
        assertEquals(true, koalaGCPastMetadata.isDeletedAndDeletable());
        assertEquals("test", koalaGCPastMetadata.getEntityType());
    }
}

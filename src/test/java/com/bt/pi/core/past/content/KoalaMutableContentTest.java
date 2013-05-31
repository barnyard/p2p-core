package com.bt.pi.core.past.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import rice.environment.Environment;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.gc.GCPast;
import rice.p2p.past.gc.GCPastContentHandle;
import rice.p2p.past.gc.GCPastMetadata;
import rice.pastry.Id;

import com.bt.pi.core.exception.KoalaContentVersionMismatchException;

public class KoalaMutableContentTest {

    Id id;
    String healthContent;
    Past mockPast;
    NodeHandle mockNodeHandle;
    Environment mockEnvironment;
    TimeSource mockTimeSource;
    String typeStr;
    HashMap<String, String> headers;
    private KoalaMutableContent content;

    @Before
    public void before() {
        id = Id.build("CAFEBEEF");
        healthContent = "yellow";
        typeStr = "String";
        mockPast = mock(Past.class);
        mockNodeHandle = mock(NodeHandle.class);
        headers = new HashMap<String, String>();
        mockTimeSource = mock(TimeSource.class);
        mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getTimeSource()).thenReturn(mockTimeSource);

        content = new KoalaPiEntityContent(id, healthContent, headers);
        content.setVersion(0);
    }

    @Test
    public void testConstructor() {
        assertEquals(id, content.getId());
        assertEquals(healthContent, content.getBody());
        assertEquals(true, content.isMutable());
    }

    @Test
    public void testGettersAndSetter() {
        // act
        content.setId(Id.build("butter"));

        // assert
        assertEquals(Id.build("butter"), content.getId());
    }

    @Test
    public void testCheckInsertForNonNetworkHealthTypes() throws Exception {
        // act & assert
        assertEquals(content, content.checkInsert(id, mock(PastContent.class)));
    }

    @Test
    public void testCheckInsert() throws Exception {
        // setup
        KoalaMutableContent newContent = new KoalaPiEntityContent(id, "blue", new HashMap<String, String>());
        newContent.setVersion(1);

        // act
        newContent.checkInsert(id, content);
    }

    @Test(expected = KoalaContentVersionMismatchException.class)
    public void testCheckInsertWithOldVersion() throws Exception {
        // setup
        KoalaMutableContent existingContent = new KoalaPiEntityContent(id, healthContent, new HashMap<String, String>());
        existingContent.setVersion(3);

        KoalaMutableContent newContent = new KoalaPiEntityContent(id, "blue", new HashMap<String, String>());
        existingContent.setVersion(0);
        // act
        newContent.checkInsert(id, existingContent);
    }

    @Test
    public void testEquals() {
        KoalaMutableContent content = new KoalaPiEntityContent(id, healthContent, new HashMap<String, String>());
        KoalaMutableContent contentDiffContent = new KoalaPiEntityContent(id, healthContent + "boo", new HashMap<String, String>());
        KoalaMutableContent contentDiffId = new KoalaPiEntityContent(Id.build("different"), healthContent, new HashMap<String, String>());
        KoalaMutableContent contentSame = new KoalaPiEntityContent(id, healthContent, new HashMap<String, String>());

        assertEquals(contentSame, content);
        assertFalse(content.equals(contentDiffId));
        assertFalse(content.equals(contentDiffContent));
    }

    @Test
    public void testGetContentHandle() {
        // setup
        long time = System.currentTimeMillis();
        when(mockTimeSource.currentTimeMillis()).thenReturn(time);
        when(mockPast.getLocalNodeHandle()).thenReturn(mockNodeHandle);
        when(mockPast.getEnvironment()).thenReturn(mockEnvironment);

        // act
        PastContentHandle contentHandle = content.getHandle(mockPast);

        // assert
        assertNotNull(contentHandle);
        assertEquals(mockNodeHandle, contentHandle.getNodeHandle());
        assertEquals(id, contentHandle.getId());
        assertTrue(contentHandle instanceof KoalaContentHandleBase);
        assertEquals(time, ((KoalaContentHandleBase) contentHandle).getTimeStamp());
        assertEquals(time + (7 * 24 * 60 * 60 * 1000), ((KoalaContentHandleBase) contentHandle).getExpiration());
    }

    @Test
    public void testMutable() {
        assertTrue(content.isMutable());
    }

    @Test
    public void testGetHandle() throws Exception {
        // setup
        GCPast gcPast = mock(GCPast.class);
        when(gcPast.getEnvironment()).thenReturn(mockEnvironment);

        // act
        GCPastContentHandle contentHandle = content.getHandle(gcPast, 1234);

        // assert
        assertNotNull(contentHandle);
    }

    @Test
    public void testGetMetadata() throws Exception {
        // act
        GCPastMetadata metadata = content.getMetadata(1234);

        // assert
        assertNotNull(metadata);
    }
}

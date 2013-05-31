package com.bt.pi.core.dht.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rice.p2p.commonapi.Id;
import rice.p2p.util.ReverseTreeMap;

import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.DhtContentHeader;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.past.content.KoalaMutableContent;
import com.bt.pi.core.past.content.KoalaPiEntityContent;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class PersistentDhtStorageTest {
    private static final String SCHEME_URI = "scheme:uri";
    private File file;
    private Id id;
    private String body;
    private HashMap<String, String> contentHeaders;

    @Mock
    private KoalaPiEntityFactory koalaPiEntityFactory;
    @InjectMocks
    private PersistentDhtStorage persistentDhtStorage = new PersistentDhtStorage();
    private ReverseTreeMap metadataMap = new ReverseTreeMap();
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PId pid;

    @Before
    public void before() throws Exception {
        persistentDhtStorage.setMetadataMap(metadataMap);
        persistentDhtStorage.setKoalaIdFactory(koalaIdFactory);
        file = mock(File.class);
        id = rice.pastry.Id.build("hoooo");
        body = "content";

        PowerMockito.mockStatic(FileUtils.class);

        contentHeaders = new HashMap<String, String>();
        contentHeaders.put(DhtContentHeader.ID, id.toStringFull());
        contentHeaders.put(DhtContentHeader.URI, SCHEME_URI);
        contentHeaders.put(DhtContentHeader.CONTENT_VERSION, "1");

        MyPiEntity piEntity = new MyPiEntity();
        when(koalaPiEntityFactory.getPiEntity(isA(String.class))).thenReturn(piEntity);

        when(koalaIdFactory.convertToPId(isA(Id.class))).thenReturn(pid);
        when(pid.forDht()).thenReturn(pid);
        when(koalaIdFactory.buildId(pid)).thenReturn(id);
    }

    @Test
    public void shouldReadData() throws Exception {
        // act
        when(FileUtils.readLines(file, "UTF8")).thenReturn(
                Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), String.format("%s: %s", DhtContentHeader.URI, SCHEME_URI), String.format("%s: %s", DhtContentHeader.CONTENT_VERSION, "1"), "", body }));
        String type = "fred";
        metadataMap.put(id, new KoalaGCPastMetadata(1, true, type));

        Serializable res = persistentDhtStorage.readData(file);

        // assert
        assertTrue(res instanceof KoalaPiEntityContent);
        assertTrue(((KoalaPiEntityContent) res).isDeletedAndDeletable());
        assertEquals(type, ((KoalaPiEntityContent) res).getEntityType());
        assertEquals(id.toStringFull(), ((KoalaMutableContent) res).getContentHeaders().get(DhtContentHeader.ID));
        assertEquals(body, ((KoalaMutableContent) res).getBody());
    }

    @Test
    public void shouldReadHeaderWithExtraSpaces() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), " MyHeader      :     value 1234    ", "", body }));

        // act
        Serializable res = persistentDhtStorage.readData(file);

        // assert
        assertTrue(res instanceof KoalaPiEntityContent);
        assertEquals("value 1234", ((KoalaMutableContent) res).getContentHeaders().get("MyHeader"));
        assertEquals(body, ((KoalaMutableContent) res).getBody());
    }

    @Test
    public void shouldReadHeaderWithNoSpaces() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), "MyHeader:value 1234", "", body }));

        // act
        Serializable res = persistentDhtStorage.readData(file);

        // assert
        assertTrue(res instanceof KoalaPiEntityContent);
        assertEquals("value 1234", ((KoalaMutableContent) res).getContentHeaders().get("MyHeader"));
        assertEquals(body, ((KoalaMutableContent) res).getBody());
    }

    @Test
    public void shouldReadHeaderWithSecondColon() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), "MyHeader:value:1234", "", body }));

        // act
        Serializable res = persistentDhtStorage.readData(file);

        // assert
        assertTrue(res instanceof KoalaPiEntityContent);
        assertEquals("value:1234", ((KoalaMutableContent) res).getContentHeaders().get("MyHeader"));
        assertEquals(body, ((KoalaMutableContent) res).getBody());
    }

    @Test(expected = DhtContentPersistenceException.class)
    public void shouldThrowOnHeaderWithNoColumn() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), "MyHeader value 1234" }));

        // act
        persistentDhtStorage.readData(file);
    }

    @Test
    public void shouldReadHeaderWhenContentEmpty() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), "\n" }));
        body = "";

        // act
        Serializable res = persistentDhtStorage.readData(file);

        // assert
        assertTrue(res instanceof KoalaPiEntityContent);
        assertEquals("", ((KoalaMutableContent) res).getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWriteObject() throws Exception {
        // setup
        KoalaPiEntityContent content = new KoalaPiEntityContent(id, body, contentHeaders);

        // act
        persistentDhtStorage.writeObject(content, contentHeaders, id, 666L, file);

        // assert
        PowerMockito.verifyStatic();
        FileUtils.writeLines(eq(file), eq("UTF8"), argThat(new ArgumentMatcher<Collection>() {
            @Override
            public boolean matches(Object argument) {
                List<String> lines = (List<String>) argument;
                if (lines.size() != 5)
                    return false;
                if (!lines.contains("Id: " + id.toStringFull()))
                    return false;
                if (!lines.contains("Uri: " + SCHEME_URI))
                    return false;
                if (!lines.contains("Content-Version: 1"))
                    return false;
                if (!lines.get(3).equals(""))
                    return false;
                if (!lines.get(4).equals(body))
                    return false;
                return true;
            }
        }));
    }

    @Test(expected = DhtContentPersistenceException.class)
    public void shouldThrowWhenWritingUnexpectedObject() throws Exception {
        // act
        persistentDhtStorage.writeObject("aaa", contentHeaders, id, 666L, file);
    }

    @Test
    public void writeMetaDataShouldDoNothing() throws Exception {
        persistentDhtStorage.writeMetadata(file, body);
    }

    @Test
    public void readMetadataShouldReaderFileHeaders() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(
                Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), String.format("%s: %s", DhtContentHeader.URI, SCHEME_URI), String.format("%s: %s", DhtContentHeader.CONTENT_VERSION, "1"), "", body }));

        // act
        Serializable result = persistentDhtStorage.readMetadata(file);

        // assert
        assertTrue(result instanceof KoalaGCPastMetadata);
        KoalaGCPastMetadata koalaContentMetadata = (KoalaGCPastMetadata) result;
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertEquals(MyPiEntity.class.getSimpleName(), koalaContentMetadata.getEntityType());
        assertFalse(koalaContentMetadata.isDeletedAndDeletable());
    }

    @Test
    public void readMetadataInvalidJson() throws Exception {
        // setup
        when(FileUtils.readLines(file, "UTF8")).thenReturn(
                Arrays.asList(new String[] { String.format("%s: %s", DhtContentHeader.ID, id.toStringFull()), String.format("%s: %s", DhtContentHeader.URI, SCHEME_URI), String.format("%s: %s", DhtContentHeader.CONTENT_VERSION, "1"), "", body }));
        when(koalaPiEntityFactory.getPiEntity(isA(String.class))).thenReturn(null);

        // act
        Serializable result = persistentDhtStorage.readMetadata(file);

        // assert
        assertTrue(result instanceof KoalaGCPastMetadata);
        KoalaGCPastMetadata koalaContentMetadata = (KoalaGCPastMetadata) result;
        assertEquals(1, koalaContentMetadata.getExpiration());
        assertNull(koalaContentMetadata.getEntityType());
        assertTrue(koalaContentMetadata.isDeletedAndDeletable());
    }

    @Test
    public void shouldRetrieveVersion() throws Exception {
        // setup
        persistentDhtStorage = new PersistentDhtStorage() {
            @Override
            protected Map<String, String> readContentHeaders(File file) throws IOException {
                return contentHeaders;
            }
        };

        // act
        long res = persistentDhtStorage.readVersion(file);

        // assert
        assertEquals(1L, res);
    }

    @Test(expected = DhtContentPersistenceException.class)
    public void shouldThrowWhenWritingObjectWithBadVersion() throws Exception {
        // setup
        KoalaPiEntityContent content = new KoalaPiEntityContent(id, body, contentHeaders);
        content.getContentHeaders().put(DhtContentHeader.CONTENT_VERSION, "-1");

        // act
        persistentDhtStorage.writeObject(content, contentHeaders, id, 666L, file);
    }

    public static class MyPiEntity implements PiEntity, Deletable {
        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUriScheme() {
            return "test";
        }

        @Override
        public long getVersion() {
            return 0;
        }

        @Override
        public void incrementVersion() {
        }

        @Override
        public void setVersion(long version) {
        }

        @Override
        public String getUrl() {
            return "test:url";
        }

        @Override
        public boolean isDeleted() {
            return false;
        }

        @Override
        public void setDeleted(boolean b) {
        }
    }
}
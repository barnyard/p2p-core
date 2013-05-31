package com.bt.pi.core.application.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.p2p.past.gc.GCId;

import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.past.content.KoalaMutableContent;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
@PowerMockIgnore({ "org.apache.commons.logging.*", "org.apache.log4j.*" })
public class LocalStorageHousekeepingHandlerTest {
    @InjectMocks
    private LocalStorageHousekeepingHandler localStorageHousekeepingHandler = new LocalStorageHousekeepingHandler();
    @Mock
    private LocalStorageScanningApplication localStorageScanningApplication;
    @Mock
    private Id id;
    @Mock
    private KoalaGCPastMetadata metadata;
    @Mock
    private PersistentDhtStorage persistentDhtStorage;
    private String tmpDir = System.getProperty("java.io.tmpdir");
    private String idAsString = "112233";
    private KoalaMutableContent koalaMutableContent;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body = "stuff";
    private String nodeId = "33333334444444444";

    @Before
    public void before() {
        headers.put("a", "b");
        koalaMutableContent = new KoalaMutableContent(id, body, headers);
        when(localStorageScanningApplication.getPersistentDhtStorage()).thenReturn(persistentDhtStorage);
        when(localStorageScanningApplication.getNodeIdFull()).thenReturn(nodeId);
        when(id.toStringFull()).thenReturn(idAsString);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult(koalaMutableContent);
                return null;
            }
        }).when(persistentDhtStorage).getObject(eq(id), isA(Continuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult(Boolean.TRUE);
                return null;
            }
        }).when(persistentDhtStorage).unstore(eq(id), isA(Continuation.class));
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(new File(String.format("%s/%s/%s/%s", tmpDir, nodeId, new SimpleDateFormat("yyyyMMdd").format(new Date()), idAsString)));
    }

    @Test
    public void testHandleDeletedAndDeletable() {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(true);

        // act
        this.localStorageHousekeepingHandler.handle(id, metadata);

        // assert
        verify(persistentDhtStorage).unstore(eq(id), isA(Continuation.class));
    }

    @Test
    public void shouldCreateArchiveWhenDeleted() throws Exception {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(true);
        this.localStorageHousekeepingHandler.setArchiveDirectory(tmpDir);

        // act
        this.localStorageHousekeepingHandler.handle(id, metadata);

        // assert
        checkFile(id.toStringFull());
    }

    @Test
    public void shouldNotUnstoreIfArchivingFailsIO() throws Exception {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(true);
        PowerMockito.mockStatic(FileUtils.class);
        PowerMockito.doThrow(new IOException("shit happens")).when(FileUtils.class);
        FileUtils.writeLines(isA(File.class), anyString(), anyList());

        // act
        this.localStorageHousekeepingHandler.handle(id, metadata);

        // assert
        verify(persistentDhtStorage, never()).unstore(eq(id), isA(Continuation.class));
    }

    @Test
    public void shouldNotUnstoreIfArchivingFailsNotMutableContent() throws Exception {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(true);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult("fred");
                return null;
            }
        }).when(persistentDhtStorage).getObject(eq(id), isA(Continuation.class));

        // act
        this.localStorageHousekeepingHandler.handle(id, metadata);

        // assert
        verify(persistentDhtStorage, never()).unstore(eq(id), isA(Continuation.class));
    }

    private void checkFile(String id) throws Exception {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File expectedFile = new File(String.format("%s/%s/%s/%s", tmpDir, nodeId, date, id));
        assertTrue(expectedFile.exists());
        List<String> readLines = FileUtils.readLines(expectedFile);
        assertEquals(3, readLines.size());
        assertEquals("a: b", readLines.get(0));
        assertEquals("", readLines.get(1));
        assertEquals(body, readLines.get(2));
    }

    @Test
    public void shouldCreateArchiveWhenDeletedGCId() throws Exception {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(true);
        this.localStorageHousekeepingHandler.setArchiveDirectory(tmpDir);
        Id localId = mock(Id.class);
        when(localId.toStringFull()).thenReturn(idAsString);
        GCId gcid = new GCId(localId, 45);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult(koalaMutableContent);
                return null;
            }
        }).when(persistentDhtStorage).getObject(eq(gcid), isA(Continuation.class));

        // act
        this.localStorageHousekeepingHandler.handle(gcid, metadata);

        // assert
        checkFile(localId.toStringFull());
    }

    @Test
    public void testHandleNotDeletedAndDeletable() {
        // setup
        when(metadata.isDeletedAndDeletable()).thenReturn(false);

        // act
        this.localStorageHousekeepingHandler.handle(id, metadata);

        // assert
        verify(persistentDhtStorage, never()).unstore(eq(id), isA(Continuation.class));
    }
}

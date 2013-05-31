package com.bt.pi.core.pastry_override;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;
import rice.environment.processing.Processor;
import rice.environment.processing.WorkRequest;
import rice.environment.random.RandomSource;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.IdRange;
import rice.p2p.commonapi.IdSet;
import rice.selector.SelectorManager;
import rice.selector.Timer;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class PersistentStorageTest {
    private KoalaIdFactory koalaIdFactory = new KoalaIdFactory();
    @Mock
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private PersistentStorage persistentStorage;
    private String rootDir = System.getProperty("java.io.tmpdir") + "/" + getClass().getSimpleName();
    @Mock
    private Environment environment;
    @Mock
    private LogManager logManager;
    @Mock
    private TimeSource timeSource;
    @Mock
    private Logger logger;
    @Mock
    private SelectorManager selectorManager;
    @Mock
    private RandomSource randomSource;
    @Mock
    private Timer timer;
    @Mock
    private Processor processor;
    private Id id1 = rice.pastry.Id.build("1111111111111");
    private Id id2 = rice.pastry.Id.build("2222222222222");
    private Id id3 = rice.pastry.Id.build("3333333333333");
    private Id id4 = rice.pastry.Id.build("4444444444444");
    @Mock
    private PId pid1;
    @Mock
    private PId pid2;
    @Mock
    private PId pid3;
    @Mock
    private PId pid4;
    private SortedMap sortedMap = new TreeMap();

    @Before
    public void before() throws IOException {
        FileUtils.deleteQuietly(new File(rootDir));
        when(environment.getLogManager()).thenReturn(logManager);
        when(logManager.getLogger((Class) any(), (String) any())).thenReturn(logger);
        when(environment.getTimeSource()).thenReturn(timeSource);
        when(environment.getSelectorManager()).thenReturn(selectorManager);
        when(selectorManager.getTimer()).thenReturn(timer);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                return null;
            }
        }).when(selectorManager).invoke(isA(Runnable.class));
        when(environment.getRandomSource()).thenReturn(randomSource);
        when(environment.getProcessor()).thenReturn(processor);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String message = (String) invocation.getArguments()[0];
                // System.out.println(message);
                return null;
            }
        }).when(logger).log(isA(String.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                WorkRequest workRequest = (WorkRequest) invocation.getArguments()[0];
                workRequest.run();
                return null;
            }
        }).when(processor).processBlockingIO(isA(WorkRequest.class));

        persistentStorage = new PersistentStorage(koalaIdFactory, koalaPiEntityFactory, "test", rootDir, 50, true, environment, 2000) {
            @Override
            protected long writeObject(Serializable obj, Serializable metadata, Id key, long version, File file) throws IOException {
                return 0;
            }

            @Override
            protected Serializable readData(File file) throws IOException {
                return null;
            }

            @Override
            protected Serializable readMetadata(File file) throws IOException {
                return null;
            }

            @Override
            protected Id readKeyFromFile(File file) throws IOException {
                return null;
            }

            @Override
            protected long readVersion(File file) throws IOException {
                return 0;
            }

            @Override
            protected void writeMetadata(File file, Serializable metadata) throws IOException {
            }
        };
    }

    @After
    public void after() throws IOException {
        FileUtils.deleteQuietly(new File(rootDir));
    }

    @Test
    public void testScanDeletesDeletedOrNullTypeEntitiesFromResult() throws Exception {
        // setup
        store(id1, setExpectationsForId(id1, false, "type1", pid1));
        store(id2, setExpectationsForId(id2, true, "type1", pid2));
        store(id3, setExpectationsForId(id3, false, null, pid3));

        // act
        IdSet result = persistentStorage.scan();

        // assert
        assertEquals(1, result.asArray().length);
        assertTrue(result.isMemberId(id1));
    }

    @Test
    public void testScanWithRangeDeletesDeletedOrNullTypeEntitiesFromResult() throws Exception {
        // setup
        store(id1, setExpectationsForId(id1, false, "type1", pid1));
        store(id2, setExpectationsForId(id2, true, "type1", pid2));
        store(id3, setExpectationsForId(id3, false, null, pid3));

        IdRange range = mock(IdRange.class);
        when(range.isEmpty()).thenReturn(false);
        when(range.getCWId()).thenReturn(id3);
        when(range.getCCWId()).thenReturn(id1);

        // act
        IdSet result = persistentStorage.scan(range);

        // assert
        assertEquals(1, result.asArray().length);
        assertTrue(result.isMemberId(id1));
    }

    private KoalaGCPastMetadata setExpectationsForId(Id id, boolean deleted, String type, PId pid) throws InterruptedException {
        KoalaGCPastMetadata metadata = new KoalaGCPastMetadata(1, deleted, type);
        sortedMap.put(id, metadata);

        // when(koalaIdFactory.convertToPId(id)).thenReturn(pid);
        // when(pid.forDht()).thenReturn(pid);
        // when(koalaIdFactory.buildId(pid)).thenReturn(id);

        return metadata;
    }

    private void store(Id id, KoalaGCPastMetadata metadata) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        persistentStorage.store(id, metadata, "test", new Continuation<Boolean, Exception>() {
            @Override
            public void receiveResult(Boolean result) {
                latch.countDown();
            }

            @Override
            public void receiveException(Exception exception) {
                System.err.println("### " + exception);
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}

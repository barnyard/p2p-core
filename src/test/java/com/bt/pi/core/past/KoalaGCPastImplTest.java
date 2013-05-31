package com.bt.pi.core.past;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;
import rice.environment.params.Parameters;
import rice.environment.random.RandomSource;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.MessageDeserializer;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;
import rice.p2p.past.PastImpl.MessageBuilder;
import rice.p2p.past.gc.GCId;
import rice.p2p.past.gc.GCIdSet;
import rice.p2p.past.gc.GCPastMetadata;
import rice.p2p.past.gc.messaging.GCCollectMessage;
import rice.p2p.past.gc.messaging.GCInsertMessage;
import rice.p2p.past.gc.messaging.GCRefreshMessage;
import rice.p2p.past.messaging.FetchHandleMessage;
import rice.p2p.past.messaging.InsertMessage;
import rice.p2p.past.messaging.PastMessage;
import rice.pastry.Id;
import rice.pastry.PastryNode;
import rice.pastry.leafset.LeafSet;
import rice.persistence.Cache;
import rice.persistence.Storage;
import rice.persistence.StorageManager;

import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.exception.KoalaContentVersionMismatchException;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.DhtContentHeader;
import com.bt.pi.core.past.content.KoalaMutableContent;
import com.bt.pi.core.past.content.KoalaPiEntityContent;
import com.bt.pi.core.past.continuation.KoalaPiEntityResultContinuation;
import com.bt.pi.core.past.internalcontinuation.KoalaPastGetHandlesInsertContinuation;
import com.bt.pi.core.past.message.InsertBackupMessage;
import com.bt.pi.core.past.message.InsertRequestMessage;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.GenericContinuationAnswer;

@SuppressWarnings("unchecked")
public class KoalaGCPastImplTest {
    private KoalaGCPastImpl koalaGCPastImpl;
    private PastMessage message;
    private KoalaMutableContent content;
    private Continuation continuation;
    private boolean invokedInsert;

    private PastryNode node;
    private String instance;
    private StorageManager storageManager;
    private MessageDeserializer koalaDeserializer;
    private Continuation getHandlesContinuation;
    private rice.p2p.commonapi.Id getHandlesId;
    private NodeHandle nodeHandle;
    private PastMessage receivedInserRequestPastmessage;
    private Continuation receivedInserRequestContinuation;
    private MessageBuilder builder;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private Continuation lookupContinuation;
    private rice.p2p.commonapi.Id lookupId;
    private rice.p2p.commonapi.Id contentId;
    private double expectedRequiredhandlePercentage = .44;
    protected rice.p2p.commonapi.Id lookupHandlesId;
    protected double requiredHandlesPercentage;
    protected Continuation lookupHandlesContinuation;
    private Id dummyId = Id.build("People are more violently opposed to fur than leather because it's safer to harass rich women than motorcycle gangs.");
    private KoalaIdFactory koalaIdFactory;
    private ArrayList<KoalaMutableContent> sentBackupContent;
    private HashSet<rice.p2p.commonapi.Id> backupIds;
    private PId realTestId;
    private InsertBackupMessage backupMessage;
    private boolean collectCalled = false;
    private rice.p2p.commonapi.Id sentId = null;
    private PastMessage sentMessage = null;
    private Continuation callBackContinuation = null;
    private Id messageId;

    @Before
    public void initKoalaGCPastImpl() {
        init();
        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, new KoalaPastPolicy(), 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory) {

            @Override
            public void insert(PastContent obj, long expiration, Continuation command) {
                invokedInsert = true;
            }

            @Override
            protected Continuation getResponseContinuation(PastMessage msg) {
                if (msg.equals(message))
                    return continuation;
                if (msg.equals(backupMessage))
                    return continuation;
                if (msg instanceof GCRefreshMessage)
                    return continuation;

                return null;
            }

            @Override
            public void lookupHandle(rice.p2p.commonapi.Id id, NodeHandle handle, Continuation command) {
            }

            @Override
            public void lookup(rice.p2p.commonapi.Id id, boolean b, Continuation command) {
                lookupId = id;
                lookupContinuation = command;
            }

            @Override
            public void getHandles(rice.p2p.commonapi.Id id, int max, Continuation command) {
                getHandlesId = id;
                getHandlesContinuation = command;
            }

            @Override
            public void lookupHandles(rice.p2p.commonapi.Id id, int max, double requiredHandles, Continuation command) {
                lookupHandlesId = id;
                requiredHandlesPercentage = requiredHandles;
                lookupHandlesContinuation = command;
            }

            @Override
            protected void sendRequest(rice.p2p.commonapi.Id id, PastMessage message, Continuation command) {
                backupIds.add(id);
                KoalaMutableContent recievedBackupContent = (KoalaMutableContent) ((InsertBackupMessage) message).getContent();
                sentBackupContent.add(recievedBackupContent);
            }

            protected void collect(java.util.SortedMap map, Continuation command) {
                collectCalled = true;
            };
        };

        koalaGCPastImpl.setRequiredReadHandlesPercentage(expectedRequiredhandlePercentage);
    }

    @Before
    public void before() {
        nodeHandle = mock(NodeHandle.class);
    }

    public void init() {
        invokedInsert = false;
        messageId = Id.build("1111567890123456789012345678901234560023");

        content = new KoalaPiEntityContent(messageId, "what", false, "type", 4, NodeScope.AVAILABILITY_ZONE, "url", 1);
        continuation = mock(Continuation.class);

        builder = mock(MessageBuilder.class);
        when(builder.buildMessage()).thenReturn(new GCInsertMessage(-1, content, 0L, nodeHandle, Id.build("dest")));

        Logger logger = mock(Logger.class);

        LogManager logManager = mock(LogManager.class);
        when(logManager.getLogger(isA(Class.class), eq(instance))).thenReturn(logger);

        Parameters parameters = mock(Parameters.class);

        RandomSource randomSource = mock(RandomSource.class);

        Environment environment = mock(Environment.class);
        when(environment.getLogManager()).thenReturn(logManager);
        when(environment.getParameters()).thenReturn(parameters);
        when(environment.getRandomSource()).thenReturn(randomSource);
        TimeSource timesource = mock(TimeSource.class);
        when(timesource.currentTimeMillis()).thenReturn(System.currentTimeMillis());
        when(environment.getTimeSource()).thenReturn(timesource);

        Endpoint endpoint = mock(Endpoint.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                koalaDeserializer = (MessageDeserializer) invocation.getArguments()[0];
                return null;
            }
        }).when(endpoint).setDeserializer(isA(MessageDeserializer.class));

        koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.buildIdFromToString(anyString())).thenAnswer(new Answer<Id>() {
            @Override
            public Id answer(InvocationOnMock invocation) throws Throwable {
                return Id.build((String) invocation.getArguments()[0]);
            }
        });

        when(koalaIdFactory.buildId(anyString())).thenAnswer(new Answer<Id>() {
            @Override
            public Id answer(InvocationOnMock invocation) throws Throwable {
                return Id.build((String) invocation.getArguments()[0]);
            }
        });

        node = mock(PastryNode.class);
        when(node.getEnvironment()).thenReturn(environment);
        when(node.buildEndpoint(isA(Application.class), eq(instance))).thenReturn(endpoint);
        when(node.getIdFactory()).thenReturn(koalaIdFactory);

        sentBackupContent = new ArrayList<KoalaMutableContent>();
        backupIds = new HashSet<rice.p2p.commonapi.Id>();

        storageManager = mock(StorageManager.class);
        koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);
    }

    @Test
    public void testDeserializer() throws IOException {
        InputBuffer inBuf = mock(InputBuffer.class);
        when(inBuf.readByte()).thenReturn(new Byte((byte) 0));

        Object message = koalaDeserializer.deserialize(inBuf, InsertRequestMessage.TYPE, InsertRequestMessage.DEFAULT_PRIORITY, mock(NodeHandle.class));

        assertTrue(message instanceof InsertRequestMessage);
    }

    @Test
    public void testDeliverRandomMessage() throws Exception {
        // setup
        message = new InsertMessage(0, content, null, null);

        // act
        koalaGCPastImpl.deliver(null, message);

        // assert
        assertFalse(invokedInsert);
    }

    @Test
    public void testDeliverGCRefresh() throws Exception {
        // setup
        GCIdSet idSet = mock(GCIdSet.class);
        when(idSet.numElements()).thenReturn(1);
        GCId id = mock(GCId.class);
        when(idSet.asArray()).thenReturn(new GCId[] { id });
        message = new GCRefreshMessage(0, idSet, null, null);

        // act
        koalaGCPastImpl.deliver(null, message);

        // assert
        verify(continuation).receiveResult(AdditionalMatchers.aryEq(new Boolean[] { Boolean.TRUE }));
        verify(storageManager, never()).exists((Id) anyObject());
    }

    @Test
    public void testDeliverGCCollect() throws Exception {
        // setup
        message = new GCCollectMessage(0, null, null);

        // act
        koalaGCPastImpl.deliver(null, message);

        // assert
        assertFalse(collectCalled);
    }

    @Test
    public void testInsert() throws Exception {
        // setup
        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, new KoalaPastPolicy(), 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory) {
            @Override
            protected void sendRequest(rice.p2p.commonapi.Id id, PastMessage message, Continuation command) {
                contentId = id;
                receivedInserRequestContinuation = command;
                receivedInserRequestPastmessage = message;
            }
        };

        // act
        koalaGCPastImpl.insert(content, 0, continuation);

        // assert
        assertEquals(contentId, content.getId());
        assertTrue(receivedInserRequestPastmessage instanceof InsertRequestMessage);
        assertEquals(receivedInserRequestContinuation, continuation);
    }

    @Test
    public void testDoInsert() {
        final Object expectedResult = new Object();
        Continuation continuation = mock(Continuation.class);
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // KoalaMutableContent existingContent = new KoalaPiEntityContent(dhtId, "what2", new HashMap<String,
                // String>());
                KoalaMutableContent existingContent = new KoalaPiEntityContent(messageId, "what2", new HashMap<String, String>());
                ((StandardContinuation) invocation.getArguments()[1]).receiveResult(existingContent);
                return null;
            }
        }).when(storageManager).getObject(eq(messageId), isA(StandardContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((StandardContinuation) invocation.getArguments()[3]).receiveResult(expectedResult);
                return null;
            }
        }).when(storageManager).store(eq(messageId), (Serializable) argThat(new ArgumentMatcher<GCPastMetadata>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof GCPastMetadata))
                    return false;
                GCPastMetadata koalaContentMetadata = (GCPastMetadata) argument;
                return koalaContentMetadata.getExpiration() == content.getVersion();
            }
        }), argThat(new ArgumentMatcher<KoalaMutableContent>() {
            @Override
            public boolean matches(Object argument) {
                if (!(argument instanceof KoalaMutableContent))
                    return false;
                KoalaMutableContent koalaMutableContent = (KoalaMutableContent) argument;
                rice.p2p.commonapi.Id id = koalaMutableContent.getId();
                return id.equals(messageId);
                // return id.toStringFull().endsWith("0001");
            }
        }), isA(StandardContinuation.class));

        // act
        koalaGCPastImpl.doInsert(content.getId(), builder, continuation, false);

        // verify
        assertEquals(messageId, getHandlesId);
        assertTrue(getHandlesContinuation instanceof KoalaPastGetHandlesInsertContinuation);
    }

    @Test
    public void testDoInsertCheckInsertFails() {
        // setup
        Continuation continuation = mock(Continuation.class);

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // KoalaMutableContent existingContent = new KoalaPiEntityContent(dhtId, "what2", new HashMap<String,
                // String>());
                KoalaMutableContent existingContent = new KoalaPiEntityContent(messageId, "what2", new HashMap<String, String>());
                existingContent.getContentHeaders().put(DhtContentHeader.CONTENT_VERSION, Long.toString(2));
                ((StandardContinuation) invocation.getArguments()[1]).receiveResult(existingContent);
                return null;
            }
        }).when(storageManager).getObject(eq(messageId), isA(StandardContinuation.class));

        // act
        koalaGCPastImpl.doInsert(content.getId(), builder, continuation, false);

        // verify
        verify(continuation).receiveException(isA(PastException.class));
    }

    @Test
    public void testGet() {
        final AtomicInteger count = new AtomicInteger(0);
        final PId idToQuery = new PiId(messageId.toStringFull(), 0);

        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, new KoalaPastPolicy(), 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory) {
            @Override
            protected Continuation getResponseContinuation(PastMessage msg) {
                if (msg.equals(message))
                    return continuation;
                return null;
            }

            @Override
            protected void getHandles(rice.p2p.commonapi.Id id, int max, Continuation command) {
                NodeHandleSet nodeSet = mock(NodeHandleSet.class);
                when(nodeSet.size()).thenReturn(3);
                command.receiveResult(nodeSet);
            }

            @Override
            public void lookupHandle(rice.p2p.commonapi.Id id, NodeHandle handle, Continuation command) {
                assertEquals(Id.build(idToQuery.toStringFull()), id);
                count.addAndGet(1);
            }
        };

        // act
        koalaGCPastImpl.get(idToQuery, continuation);

        // assert
        assertEquals(3, count.intValue());
    }

    @Test
    public void testGetMultiContinuation() {
        // setup
        final PId idToQuery = new PiId(messageId.toStringFull(), 0);

        // act
        koalaGCPastImpl.get(idToQuery, continuation);
    }

    @Test
    public void testGetAny() {
        // setup
        final PId idToQuery = new PiId(messageId.toStringFull(), 0);

        // act
        koalaGCPastImpl.getAny(idToQuery, continuation);

        // assert
        assertTrue(lookupContinuation instanceof KoalaPiEntityResultContinuation);
        assertEquals(messageId, lookupId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutThrowsExceptionOnNonPersistable() {

        // act
        koalaGCPastImpl.put(new PiId("testPutException", 0), new NonPersistable(), continuation);
    }

    @Test
    public void testGetSuccessThreshold() {
        // setup
        koalaGCPastImpl.setSuccessfullInsertThreshold(94.3);

        // act
        assertEquals(94.3, koalaGCPastImpl.getSuccessfulInsertThreshold(), 0);
    }

    @Test
    public void testDeliverFetchHandleMessage() {
        // setup
        Id id = mock(Id.class);
        FetchHandleMessage message = mock(FetchHandleMessage.class);
        when(message.getId()).thenReturn(id);
        PId pid = mock(PId.class);
        when(koalaIdFactory.convertToPId(id)).thenReturn(pid);
        when(pid.forDht()).thenReturn(pid);
        when(koalaIdFactory.buildId(pid)).thenReturn(id);

        // act
        koalaGCPastImpl.deliver(null, message);

        // assert
        verify(storageManager).getObject(eq(id), isA(Continuation.class));
    }

    @Test
    public void testGetGlobalIds() {
        // setup
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        koalaGCPastImpl.setKoalaIdFactory(koalaIdFactory);
        PId testId = koalaGCPastImpl.getKoalaIdFactory().buildPId("R. Lopez");

        // act
        Set<String> globalIds = koalaGCPastImpl.generateBackupIds(4, NodeScope.GLOBAL, testId);

        // asssert
        assertEquals(4, globalIds.size());
        assertSetEndsWithBackupIdentifier(globalIds);
    }

    private void assertSetEndsWithBackupIdentifier(Set<String> ids) {
        for (String str : ids) {
            assertEquals(1, new BigInteger(str.substring(39), 16).mod(new BigInteger("2")).intValue());
        }
    }

    @Test
    public void testGetRegionalIds() {
        // setup
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        koalaGCPastImpl.setKoalaIdFactory(koalaIdFactory);
        PId testId = koalaGCPastImpl.getKoalaIdFactory().buildPId("R. Lopez");

        // act
        Set<String> ids = koalaGCPastImpl.generateBackupIds(4, NodeScope.REGION, testId);

        assertEquals(4, ids.size());
        assertSetEndsWithBackupIdentifier(ids);
        assertSetContains(ids, testId.getIdAsHex().substring(4, testId.getIdAsHex().length() - 1));
    }

    private void assertSetContains(Set<String> ids, String mustContain) {
        for (String str : ids) {
            assertTrue(str.toLowerCase().contains(mustContain.toLowerCase()));
        }
    }

    @Test
    public void testGetAvailabilityZoneIds() {
        // setup
        KoalaIdFactory koalaIdFactory = new KoalaIdFactory(0, 0);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);
        koalaGCPastImpl.setKoalaIdFactory(koalaIdFactory);
        PId testId = koalaGCPastImpl.getKoalaIdFactory().buildPId("YEAHBOB");

        // act
        Set<String> ids = koalaGCPastImpl.generateBackupIds(4, NodeScope.AVAILABILITY_ZONE, testId);

        // assert
        assertEquals(4, ids.size());
        assertSetEndsWithBackupIdentifier(ids);
        assertSetContains(ids, testId.getIdAsHex().substring(5, testId.getIdAsHex().length() - 1));
    }

    @Test
    public void testGetGlobalBackupIDs() {
        // setup
        koalaGCPastImpl.setKoalaIdFactory(new KoalaIdFactory(0, 0));
        realTestId = (PId) koalaGCPastImpl.getKoalaIdFactory().buildPIdFromHexString("1234567890123456789012345678901234567892");

        // act
        Set<String> ids = koalaGCPastImpl.generateBackupIds(4, NodeScope.GLOBAL, realTestId);

        // assert
        assertSetEndsWithBackupIdentifier(ids);
        assertSetContains(ids, "34567890123456789012345678901234560013");
    }

    @Test
    public void testBackupContent() {
        // setup
        PId pid = mock(PId.class);
        when(pid.asBackupId()).thenReturn(pid);
        when(pid.getIdAsHex()).thenReturn(content.getId().toStringFull());
        when(koalaIdFactory.convertToPId(eq(content.getId()))).thenReturn(pid);

        // act
        koalaGCPastImpl.backupContent(4, NodeScope.REGION, content);

        // assert
        assertEquals(4, backupIds.size());
        for (KoalaMutableContent sentContent : sentBackupContent) {
            assertEquals(sentContent.getBody(), content.getBody());
            assertEquals(sentContent.getContentHeaders().get(DhtContentHeader.CONTENT_TYPE), content.getContentHeaders().get(DhtContentHeader.CONTENT_TYPE));
            assertEquals(sentContent.getContentHeaders().get(DhtContentHeader.ID), sentContent.getId().toStringFull());
            assertEquals(sentContent.getVersion(), content.getVersion());
            assertNotSame(sentContent.getId(), content.getId());
        }
    }

    @Test
    public void testDeliverOfBackupMessageIfItIsNewer() {
        // setup
        content.getContentHeaders().put(DhtContentHeader.CONTENT_VERSION, Long.toString(2));
        backupMessage = new InsertBackupMessage(85, content, -1, nodeHandle, content.getId());

        // Make sure that the existing content is older
        KoalaMutableContent existingContent = new KoalaPiEntityContent(Id.build("ExisingContent"), "existingContent", new HashMap<String, String>());
        existingContent.getContentHeaders().put(DhtContentHeader.CONTENT_VERSION, Long.toString(1));

        GenericContinuationAnswer<KoalaMutableContent> g = new GenericContinuationAnswer<KoalaMutableContent>(existingContent);
        doAnswer(g).when(storageManager).getObject(eq(content.getId()), isA(StandardContinuation.class));

        ArgumentCaptor<GCPastMetadata> metadataArgument = ArgumentCaptor.forClass(GCPastMetadata.class);

        // act
        koalaGCPastImpl.deliver(content.getId(), backupMessage);

        // assert
        verify(storageManager).store(eq(content.getId()), metadataArgument.capture(), eq(content), isA(StandardContinuation.class));
        assertEquals(2, metadataArgument.getValue().getExpiration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFetchNotGCId() {
        // act
        koalaGCPastImpl.fetch(mock(Id.class), mock(NodeHandle.class), mock(Continuation.class));
    }

    @Test
    public void testShouldThrowExceptionIfBackupMessageIsOlderOrSameAsTheExistingBackedUpMessage() {
        backupMessage = new InsertBackupMessage(85, content, -1, nodeHandle, content.getId());

        GenericContinuationAnswer<KoalaMutableContent> g = new GenericContinuationAnswer<KoalaMutableContent>(content);
        doAnswer(g).when(storageManager).getObject(eq(content.getId()), isA(StandardContinuation.class));

        // act
        koalaGCPastImpl.deliver(content.getId(), backupMessage);

        // assert
        verify(continuation).receiveException(isA(KoalaContentVersionMismatchException.class));
    }

    @Test
    public void testFetchNotInStorage() throws Exception {
        // setup
        KoalaPastPolicy koalaPastPolicy = mock(KoalaPastPolicy.class);
        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, koalaPastPolicy, 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory);

        Id id = mock(Id.class);
        GCId gcId = mock(GCId.class);
        when(gcId.getId()).thenReturn(id);
        when(storageManager.exists(id)).thenReturn(false);
        NodeHandle hint = mock(NodeHandle.class);
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation continuation = (StandardContinuation) invocation.getArguments()[4];
                continuation.receiveResult(content);
                latch.countDown();
                return null;
            }
        }).when(koalaPastPolicy).fetch(eq(id), eq(hint), (Cache) isNull(), isA(Past.class), isA(StandardContinuation.class));

        Storage storage = mock(Storage.class);
        when(storageManager.getStorage()).thenReturn(storage);

        // act
        koalaGCPastImpl.fetch(gcId, hint, mock(Continuation.class));

        // assert
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        verify(storage).store(eq(id), isA(GCPastMetadata.class), eq(content), isA(Continuation.class));
    }

    @Test
    public void shouldNotStoreLocallyWhenReplicationFetchingIfDeletableAndDeleted() throws Exception {
        // setup
        KoalaPastPolicy koalaPastPolicy = mock(KoalaPastPolicy.class);
        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, koalaPastPolicy, 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory);

        Id id = mock(Id.class);
        GCId gcId = mock(GCId.class);
        when(gcId.getId()).thenReturn(id);
        when(storageManager.exists(id)).thenReturn(false);
        NodeHandle hint = mock(NodeHandle.class);
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation continuation = (StandardContinuation) invocation.getArguments()[4];
                continuation.receiveResult(content);
                latch.countDown();
                return null;
            }
        }).when(koalaPastPolicy).fetch(eq(id), eq(hint), (Cache) isNull(), isA(Past.class), isA(StandardContinuation.class));

        Storage storage = mock(Storage.class);
        when(storageManager.getStorage()).thenReturn(storage);

        String json = "what";
        content = new KoalaPiEntityContent(messageId, json, true, "text", 1, NodeScope.GLOBAL, "", 0);

        // act
        koalaGCPastImpl.fetch(gcId, hint, mock(Continuation.class));

        // assert
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        verify(storage, never()).store(eq(id), isA(GCPastMetadata.class), eq(content), isA(Continuation.class));
    }

    @Test
    public void shouldNotStoreLocallyWhenReplicationFetchingIfNullType() throws Exception {
        // setup
        KoalaPastPolicy koalaPastPolicy = mock(KoalaPastPolicy.class);
        koalaGCPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, koalaPastPolicy, 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory);

        Id id = mock(Id.class);
        GCId gcId = mock(GCId.class);
        when(gcId.getId()).thenReturn(id);
        when(storageManager.exists(id)).thenReturn(false);
        NodeHandle hint = mock(NodeHandle.class);
        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation continuation = (StandardContinuation) invocation.getArguments()[4];
                continuation.receiveResult(content);
                latch.countDown();
                return null;
            }
        }).when(koalaPastPolicy).fetch(eq(id), eq(hint), (Cache) isNull(), isA(Past.class), isA(StandardContinuation.class));

        Storage storage = mock(Storage.class);
        when(storageManager.getStorage()).thenReturn(storage);

        String json = "what";
        content = new KoalaPiEntityContent(messageId, json, false, null, 1, NodeScope.GLOBAL, "", 0);

        // act
        koalaGCPastImpl.fetch(gcId, hint, mock(Continuation.class));

        // assert
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        verify(storage, never()).store(eq(id), isA(GCPastMetadata.class), eq(content), isA(Continuation.class));
    }

    @Test
    public void testGetLeafSet() {
        LeafSet leafset = mock(LeafSet.class);
        when(node.getLeafSet()).thenReturn(leafset);

        // act
        LeafSet result = koalaGCPastImpl.getLeafSet();

        // verify
        assertEquals(leafset, result);
    }

    @Test
    public void testReadObjectFromStorage() {
        // act
        koalaGCPastImpl.readObjectFromStorage(dummyId, continuation);

        // verify
        verify(storageManager).getObject(dummyId, continuation);
    }

    @Test
    public void testGetMessageContinuation() {
        // setup
        message = mock(PastMessage.class);

        // act
        Continuation actual = koalaGCPastImpl.getMessageResponseContinuation(message);

        // verify
        assertEquals(continuation, actual);
    }

    @Test
    public void testGetLocalNodeId() {
        // setup
        when(node.getId()).thenReturn(contentId);

        rice.p2p.commonapi.Id id = koalaGCPastImpl.getLocalNodeId();

        assertEquals(contentId, id);
    }

    @Test
    public void testGenerateBackups() {
        PId pid = new PiId("0234567890123456789012345678901234567890", 1);

        // act
        SortedSet<String> backupIds = koalaGCPastImpl.generateBackupIds(16, NodeScope.GLOBAL, pid);

        String firstBackup = backupIds.first();
        assertTrue(firstBackup.endsWith("1"));
        for (String id : backupIds) {
            assertEquals(firstBackup.substring(1), id.substring(1));
        }
    }

    @Test
    public void testInsertGetsVersion() {
        // setup
        KoalaGCPastImpl localGcPastImpl = new KoalaGCPastImpl(node, storageManager, null, 0, instance, new KoalaPastPolicy(), 0, storageManager, KoalaNode.DEFAULT_NUMBER_OF_DHT_BACKUPS, koalaIdFactory, koalaPiEntityFactory) {
            @Override
            protected void sendRequest(rice.p2p.commonapi.Id id, PastMessage message, Continuation command) {
                sentId = id;
                sentMessage = message;
                callBackContinuation = command;
            }
        };
        PastContent content = new KoalaPiEntityContent(Id.build("bob"), "the builder", false, "text", 0, NodeScope.AVAILABILITY_ZONE, "tv:series", 18);
        Continuation command = mock(Continuation.class);

        // act
        localGcPastImpl.insert(content, command);

        // assert
        assertEquals(sentId, Id.build("bob"));
        assertTrue(sentMessage instanceof InsertRequestMessage);
        assertEquals(18, ((InsertRequestMessage) sentMessage).getExpiration());
        assertEquals(content, ((InsertRequestMessage) sentMessage).getContent());
        assertEquals(command, callBackContinuation);
    }

    @Test
    public void testSetRequiredReadHandlesPercentage() {
        double d = 99.39;

        // act
        koalaGCPastImpl.setRequiredReadHandlesPercentage(d);

        // assert
        assertEquals(d, koalaGCPastImpl.getRequiredReadHandlesThreshold(), 0.0);
    }

    class NonPersistable extends PiEntityBase {
        public NonPersistable() {
        }

        @Override
        public String getType() {
            return "NonPersistable";
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
            return "url:NonPersistable";
        }

        @Override
        public String getUriScheme() {
            return null;
        }
    }
}

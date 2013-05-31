package com.bt.pi.core.past;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.messaging.FetchHandleMessage;
import rice.p2p.past.messaging.PastMessage;
import rice.pastry.Id;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.leafset.LeafSet;

import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaPiEntityContent;
import com.bt.pi.core.past.message.FetchBackupHandleMessage;
import com.bt.pi.core.past.message.InsertBackupMessage;
import com.bt.pi.core.scope.NodeScope;

@SuppressWarnings("unchecked")
public class PiBackupHelperTest {
    private PiBackupHelper piBackupHelper;
    private KoalaDHTStorage koalaDHTStorage;
    private Id pastId = Id.build("content");
    private int numberOfBackups = 2;
    private SortedSet<String> backupIds;
    private PastryNode node;
    private FetchBackupHandleMessage fbhmsg;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private NodeHandle localNodeHandle;
    private Id firstBackupId;
    private Object result;
    private Exception exception;
    private PastContent content;
    private PastContentHandle contentHandle;
    private Continuation messageContinuation;
    private FetchHandleMessage fmsg;
    private LeafSet leafset;
    private KoalaIdFactory koalaIdFactory;

    @Before
    public void before() {
        backupIds = new TreeSet<String>();
        backupIds.add("PAM");
        backupIds.add("SAM");

        messageContinuation = mock(Continuation.class);

        result = new Object();
        exception = new Exception();

        firstBackupId = Id.build(backupIds.first());

        leafset = mock(LeafSet.class);
        when(leafset.asList()).thenReturn(new ArrayList<NodeHandle>());

        node = mock(PastryNode.class);
        when(node.getLeafSet()).thenReturn(leafset);
        when(node.getId()).thenReturn(Id.build("coolBeans"));

        fmsg = mock(FetchHandleMessage.class);
        when(fmsg.getId()).thenReturn(pastId);

        koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);

        koalaIdFactory = new KoalaIdFactory(99, 99);
        koalaIdFactory.setKoalaPiEntityFactory(koalaPiEntityFactory);

        koalaDHTStorage = mock(KoalaDHTStorage.class);
        when(koalaDHTStorage.generateBackupIds(eq(numberOfBackups), eq(NodeScope.REGION), eq(koalaIdFactory.convertToPId(pastId)))).thenReturn(backupIds);
        when(koalaDHTStorage.getKoalaIdFactory()).thenReturn(koalaIdFactory);
        when(koalaDHTStorage.getLocalNodeId()).thenReturn(Id.build("coolBeans"));
        when(koalaDHTStorage.getLeafSet()).thenReturn(leafset);
        when(koalaDHTStorage.getMessageResponseContinuation(eq(fmsg))).thenReturn(messageContinuation);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).schedule(isA(Runnable.class), anyInt(), isA(TimeUnit.class));

        fbhmsg = mock(FetchBackupHandleMessage.class);
        when(fbhmsg.getId()).thenReturn(pastId);

        localNodeHandle = mock(NodeHandle.class);

        contentHandle = mock(PastContentHandle.class);
        content = mock(PastContent.class);
        when(content.getHandle(eq(koalaDHTStorage))).thenReturn(contentHandle);

        piBackupHelper = new PiBackupHelper(koalaDHTStorage, numberOfBackups, koalaPiEntityFactory);
        piBackupHelper.setExecutor(executor);
    }

    @Test
    public void testHandleDataInsertion() {
        // setup
        final KoalaPiEntityContent pastContent = new KoalaPiEntityContent(pastId, "json!", false, "text", 1, NodeScope.REGION, "entity:url", 0L);

        // act
        piBackupHelper.handleDataInsertion(pastContent);

        // assert
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                rice.p2p.commonapi.Id id = (rice.p2p.commonapi.Id) invocation.getArguments()[0];
                assertEquals(id, koalaDHTStorage.getKoalaIdFactory().buildIdFromToString("FAM"));
                InsertBackupMessage insertBackupMessage = (InsertBackupMessage) invocation.getArguments()[1];
                assertEquals(pastContent, insertBackupMessage.getContent());
                return null;
            }
        }).when(koalaDHTStorage).sendPastMessage(isA(rice.p2p.commonapi.Id.class), isA(InsertBackupMessage.class), isA(Continuation.class));
    }

    @Test
    public void testHandleDataInsertionSendsFourMessages() {
        // setup
        KoalaPiEntityContent pastContent = new KoalaPiEntityContent(pastId, "json!", false, "text", numberOfBackups, NodeScope.REGION, "entity:url", 0L);

        // act
        piBackupHelper.handleDataInsertion(pastContent);

        // assert
        verify(koalaDHTStorage, times(numberOfBackups)).sendPastMessage(isA(rice.p2p.commonapi.Id.class), isA(InsertBackupMessage.class), isA(Continuation.class));
    }

    @Test
    public void testHandleFetchHandleMessageReturnsLocalPastContentHandle() {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((StandardContinuation) invocation.getArguments()[1]).receiveResult(content);
                return null;
            }
        }).when(koalaDHTStorage).readObjectFromStorage(eq(pastId), isA(StandardContinuation.class));

        // act
        piBackupHelper.handleFetchHandleMessage(fmsg);

        // verify
        verify(messageContinuation).receiveResult(contentHandle);
    }

    @Test
    public void testHandleFetchHandleMessageCallsRelayToBackup() {
        // setup
        pastId = Id.build("1111567890123456789012345678901234560002");
        when(fmsg.getId()).thenReturn(pastId);

        final AtomicBoolean relayCalled = new AtomicBoolean(false);
        PiBackupHelper backupHelper = new PiBackupHelper(koalaDHTStorage, numberOfBackups, koalaPiEntityFactory) {
            protected void relayFetchHandleMessageToBackup(FetchHandleMessage fetchHandleMessage, Continuation parentContinuation, PId messageId) {
                if (fetchHandleMessage.equals(fmsg) && parentContinuation.equals(messageContinuation)) {
                    relayCalled.set(true);
                }
            }
        };
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((StandardContinuation) invocation.getArguments()[1]).receiveResult(null);
                return null;
            }
        }).when(koalaDHTStorage).readObjectFromStorage(eq(pastId), isA(StandardContinuation.class));

        // act
        backupHelper.handleFetchHandleMessage(fmsg);

        // verify
        assertTrue(relayCalled.get());
    }

    @Test
    public void testHandleFetchBackupHandleMessageCallsParentwithResponse() {
        // setup
        pastId = Id.build("1111567890123456789012345678901234560002");
        // final Id dhtId = Id.build("1111567890123456789012345678901234560000");
        when(fbhmsg.getId()).thenReturn(pastId);

        Continuation parentContinuation = mock(Continuation.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[1]).receiveException(exception);
                ((Continuation) invocation.getArguments()[1]).receiveResult(result);
                return null;
            }
        }).when(koalaDHTStorage).readObjectFromStorage(eq(pastId), isA(StandardContinuation.class));
        when(koalaDHTStorage.getMessageResponseContinuation(eq(fbhmsg))).thenReturn(parentContinuation);

        // act
        piBackupHelper.handleFetchBackupHandleMessage(fbhmsg);

        // verify
        verify(parentContinuation).receiveResult(null);
        verify(parentContinuation).receiveException(exception);
    }

    @Test
    public void testHandleFetchBackupHandleMessageCallsParentwithContentHandle() {
        // setup
        pastId = Id.build("1111567890123456789012345678901234560002");
        when(fbhmsg.getId()).thenReturn(pastId);

        Continuation parentContinuation = mock(Continuation.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation) invocation.getArguments()[1]).receiveResult(content);
                return null;
            }
        }).when(koalaDHTStorage).readObjectFromStorage(eq(pastId), isA(StandardContinuation.class));
        when(koalaDHTStorage.getMessageResponseContinuation(eq(fbhmsg))).thenReturn(parentContinuation);

        // act
        piBackupHelper.handleFetchBackupHandleMessage(fbhmsg);

        // verify
        verify(parentContinuation).receiveResult(contentHandle);
        verify(parentContinuation, never()).receiveException(exception);
    }

    @Test
    public void testRelayFetchHandleMessage() {
        // setup
        ArgumentCaptor<FetchBackupHandleMessage> fetchBackupHandleMsgCaptor = ArgumentCaptor.forClass(FetchBackupHandleMessage.class);
        StandardContinuation continuation = mock(StandardContinuation.class);
        FetchHandleMessage fhmsg = mock(FetchHandleMessage.class);
        when(fhmsg.getId()).thenReturn(pastId);
        when(koalaDHTStorage.generateBackupIds(eq(numberOfBackups), eq(NodeScope.REGION), eq(koalaIdFactory.convertToPId(pastId)))).thenReturn(backupIds);
        when(koalaDHTStorage.getLocalNodeHandle()).thenReturn(localNodeHandle);
        PId messageId = mock(PId.class);
        when(messageId.getScope()).thenReturn(NodeScope.REGION);

        // act
        piBackupHelper.relayFetchHandleMessageToBackup(fhmsg, continuation, messageId);

        // verify
        verify(koalaDHTStorage).sendPastMessage(isA(Id.class), fetchBackupHandleMsgCaptor.capture(), isA(StandardContinuation.class));
        assertEquals(localNodeHandle, fetchBackupHandleMsgCaptor.getValue().getSource());
        assertEquals(firstBackupId, fetchBackupHandleMsgCaptor.getValue().getDestination());
        assertEquals(firstBackupId, fetchBackupHandleMsgCaptor.getValue().getId());
    }

    @Test
    public void testRelayFetchHandleMessageWhenPositionIsHigherThanNumberOfBackups() {
        // setup
        Id closestId = Id.build(backupIds.first());
        when(fmsg.getId()).thenReturn(closestId);
        BigInteger bigInteger = new BigInteger(closestId.toStringFull(), 16);
        // create a bunch of ID's with distances away from the closest
        ArrayList<Id> idsInLeafset = new ArrayList<Id>();
        idsInLeafset.add(Id.build(bigInteger.toString(16)));
        idsInLeafset.add(Id.build(bigInteger.add(BigInteger.ONE).toString(16)));
        idsInLeafset.add(Id.build(bigInteger.add(BigInteger.ONE).add(BigInteger.ONE).toString(16)));
        idsInLeafset.add(Id.build(bigInteger.add(BigInteger.TEN).toString(16)));
        idsInLeafset.add(Id.build(bigInteger.add(BigInteger.TEN).add(BigInteger.ONE).toString(16)));
        ArgumentCaptor<FetchBackupHandleMessage> fetchBackupHandleMsgCaptor = ArgumentCaptor.forClass(FetchBackupHandleMessage.class);
        StandardContinuation continuation = mock(StandardContinuation.class);
        FetchHandleMessage fhmsg = mock(FetchHandleMessage.class);
        when(fhmsg.getId()).thenReturn(pastId);
        SortedSet<String> testBackupIdSet = new TreeSet<String>();
        testBackupIdSet.add(idsInLeafset.get(0).toStringFull());
        when(koalaDHTStorage.generateBackupIds(eq(4), eq(NodeScope.REGION), eq(koalaIdFactory.convertToPId(pastId)))).thenReturn(testBackupIdSet);
        when(koalaDHTStorage.getLocalNodeHandle()).thenReturn(localNodeHandle);
        when(koalaDHTStorage.getLocalNodeId()).thenReturn(idsInLeafset.get(4));
        NodeHandle leafSetNodeHandle = mock(NodeHandle.class);
        when(leafSetNodeHandle.getId()).thenReturn(idsInLeafset.get(0)).thenReturn(idsInLeafset.get(1)).thenReturn(idsInLeafset.get(2)).thenReturn(idsInLeafset.get(3)).thenReturn(idsInLeafset.get(4));
        ArrayList<NodeHandle> leafSetNodeHandles = new ArrayList<NodeHandle>();
        leafSetNodeHandles.add(leafSetNodeHandle);
        leafSetNodeHandles.add(leafSetNodeHandle);
        leafSetNodeHandles.add(leafSetNodeHandle);
        when(leafset.asList()).thenReturn(leafSetNodeHandles);
        PId messageId = mock(PId.class);
        when(messageId.getScope()).thenReturn(NodeScope.REGION);

        // act
        piBackupHelper.relayFetchHandleMessageToBackup(fhmsg, continuation, messageId);

        // verify
        verify(koalaDHTStorage).sendPastMessage(eq(idsInLeafset.get(0)), fetchBackupHandleMsgCaptor.capture(), isA(StandardContinuation.class));
        assertEquals(localNodeHandle, fetchBackupHandleMsgCaptor.getValue().getSource());
        assertEquals(firstBackupId, fetchBackupHandleMsgCaptor.getValue().getDestination());
        assertEquals(firstBackupId, fetchBackupHandleMsgCaptor.getValue().getId());
    }

    @Test
    public void testRelayFetchHandleMessageReturnsResultToParentContinuation() {
        // setup
        StandardContinuation parentContinuation = mock(StandardContinuation.class);
        FetchHandleMessage fhmsg = mock(FetchHandleMessage.class);
        when(fhmsg.getId()).thenReturn(pastId);
        when(koalaDHTStorage.generateBackupIds(eq(4), eq(NodeScope.REGION), eq(koalaIdFactory.convertToPId(pastId)))).thenReturn(backupIds);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation<Object, Exception> continuation = (StandardContinuation) invocation.getArguments()[2];
                continuation.receiveResult(result);
                continuation.receiveException(exception);
                return null;
            }
        }).when(koalaDHTStorage).sendPastMessage(isA(Id.class), isA(PastMessage.class), isA(Continuation.class));
        PId messageId = mock(PId.class);
        when(messageId.getScope()).thenReturn(NodeScope.REGION);

        // act
        piBackupHelper.relayFetchHandleMessageToBackup(fhmsg, parentContinuation, messageId);

        // verify
        verify(parentContinuation).receiveException(exception);
        verify(parentContinuation).receiveResult(result);
    }
}

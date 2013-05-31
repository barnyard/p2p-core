package com.bt.pi.core.past.internalcontinuation;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.Continuation.NamedContinuation;
import rice.p2p.commonapi.NodeHandleSet;
import rice.p2p.past.PastImpl.MessageBuilder;
import rice.p2p.past.gc.messaging.GCInsertMessage;
import rice.p2p.past.messaging.PastMessage;
import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.past.KoalaGCPastImpl;
import com.bt.pi.core.past.content.KoalaPiEntityContent;

public class KoalaPastGetHandlesInsertContinuationTest {

    private NodeHandleSet nodeHandleSet;
    private KoalaPastGetHandlesInsertContinuation koalaPastGetHandlesInsertContinuation;
    @SuppressWarnings("unchecked")
    private Continuation parentContinuation;
    private Id pastContentId = Id.build("KoalaPastGetHandlesInsertContinuationTest-" + System.currentTimeMillis());
    private MessageBuilder builder;
    private KoalaGCPastImpl past;
    private NodeHandle localNodeHandle;
    private NodeHandle otherNodeHandle;

    @Before
    public void before() {
        localNodeHandle = mock(NodeHandle.class);
        when(localNodeHandle.getId()).thenReturn(Id.build("local"));

        otherNodeHandle = mock(NodeHandle.class);
        when(otherNodeHandle.getId()).thenReturn(Id.build("other"));

        nodeHandleSet = mock(NodeHandleSet.class);
        when(nodeHandleSet.getHandle(0)).thenReturn(localNodeHandle);
        when(nodeHandleSet.getHandle(1)).thenReturn(otherNodeHandle);
        when(nodeHandleSet.size()).thenReturn(2);

        past = mock(KoalaGCPastImpl.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                return null;
            }

        }).when(past).sendPastRequest(isA(NodeHandle.class), isA(PastMessage.class), isA(Continuation.class));

        parentContinuation = mock(Continuation.class);

        builder = mock(MessageBuilder.class);
        when(builder.buildMessage()).thenReturn(new GCInsertMessage(-1, new KoalaPiEntityContent(pastContentId, "test", new HashMap<String, String>()), 0L, localNodeHandle, Id.build("dest")));

        koalaPastGetHandlesInsertContinuation = new KoalaPastGetHandlesInsertContinuation(past, parentContinuation, pastContentId, builder, false);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReceiveResult() {

        // act
        koalaPastGetHandlesInsertContinuation.receiveResult(nodeHandleSet);

        // verify
        verify(past, times(1)).sendPastRequest(eq(localNodeHandle), isA(GCInsertMessage.class), (Continuation) isA(NamedContinuation.class));
    }

}

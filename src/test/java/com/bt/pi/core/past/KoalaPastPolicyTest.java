package com.bt.pi.core.past;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.Continuation.StandardContinuation;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastContentHandle;
import rice.persistence.Cache;

import com.bt.pi.core.exception.KoalaException;
import com.bt.pi.core.past.content.KoalaContentHandleBase;
import com.bt.pi.core.past.continuation.KoalaHandleVersionComparer;

@RunWith(MockitoJUnitRunner.class)
public class KoalaPastPolicyTest {
    @InjectMocks
    private KoalaPastPolicy koalaPastPolicy = new KoalaPastPolicy();
    @Mock
    private KoalaHandleVersionComparer koalaHandleVersionComparer;
    @Mock
    private PastContent content;
    @Mock
    private Id id;
    @SuppressWarnings("unchecked")
    @Mock
    private Continuation command;
    @Mock
    private Past past;
    @Mock
    private Cache backup;
    @Mock
    private NodeHandle hint;
    private int replicationFactor = 2;
    protected PastContentHandle[] contentHandleArray = new PastContentHandle[1];
    @Mock
    private KoalaContentHandleBase contentHandle;

    @Test
    public void testAllowInsert() {
        assertTrue(koalaPastPolicy.allowInsert(content));
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    @Test
    public void testFetch() {
        // setup
        when(past.getReplicationFactor()).thenReturn(replicationFactor);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation continuation = (StandardContinuation) invocation.getArguments()[2];
                continuation.receiveResult(contentHandleArray);
                return null;
            }
        }).when(past).lookupHandles(eq(id), eq(replicationFactor + 1), isA(StandardContinuation.class));

        when(koalaHandleVersionComparer.getLatestVersion(contentHandleArray)).thenReturn(contentHandle);

        when(contentHandle.getId()).thenReturn(id);
        when(id.toStringFull()).thenReturn("handleID");

        // act
        koalaPastPolicy.fetch(id, hint, backup, past, command);

        // assert
        verify(past).fetch(eq(contentHandle), isA(Continuation.class));
    }

    @SuppressWarnings( { "unchecked", "rawtypes" })
    @Test
    public void testFetchEmptyResult() {
        // setup
        when(past.getReplicationFactor()).thenReturn(replicationFactor);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StandardContinuation continuation = (StandardContinuation) invocation.getArguments()[2];
                continuation.receiveResult(null);
                return null;
            }
        }).when(past).lookupHandles(eq(id), eq(replicationFactor + 1), isA(StandardContinuation.class));

        // act
        koalaPastPolicy.fetch(id, hint, backup, past, command);

        // assert
        verify(koalaHandleVersionComparer, never()).getLatestVersion(contentHandleArray);
        verify(past, never()).fetch(eq(contentHandle), isA(Continuation.class));
        verify(command).receiveException(isA(KoalaException.class));
    }
}

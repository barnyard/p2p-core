package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.Continuation;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.past.KoalaDHTStorage;

@SuppressWarnings("unchecked")
public class UnconditionalDhtWriterTest {
    private UnconditionalDhtWriter dhtWriter;
    private KoalaDHTStorage koalaDhtStorage;
    private PId id;
    private PiEntity entityToWrite;
    private ThreadPoolTaskExecutor executor;
    private PiEntity valueWritten;

    @Before
    public void before() {
        id = new PiId("00000000000000000000000000000000000000000000", 0);
        entityToWrite = mock(PiEntity.class);
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        koalaDhtStorage = mock(KoalaDHTStorage.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                continuation.receiveResult(new Boolean[] { true });
                return null;
            }
        }).when(koalaDhtStorage).put(eq(id), isA(PiEntity.class), isA(Continuation.class));
        dhtWriter = new UnconditionalDhtWriter(executor, koalaDhtStorage);
    }

    @Test
    public void shouldSupportUnconditionalOverwrite() {
        // act
        dhtWriter.put(id, entityToWrite);

        // assert
        verify(koalaDhtStorage, never()).get(any(PId.class), any(Continuation.class));
        verify(koalaDhtStorage).put(eq(id), eq(entityToWrite), isA(Continuation.class));
        assertEquals(entityToWrite, dhtWriter.getValueWritten());
    }

    @Test(expected = RuntimeException.class)
    public void shouldPassExceptionBackThroughWhenOneOccurs() {
        // setup
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(koalaDhtStorage).put(eq(id), isA(PiEntity.class), isA(Continuation.class));

        // act
        dhtWriter.put(id, entityToWrite);
    }

    @Test
    public void shouldBeAbleToDoAsyncUnconditionalWrite() throws InterruptedException {
        // setup
        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtWriter.put(id, entityToWrite, new Continuation<PiEntity, Exception>() {
            @Override
            public void receiveException(Exception e) {
            }

            @Override
            public void receiveResult(PiEntity result) {
                valueWritten = result;
                cdl.countDown();
            }
        });
        cdl.await(5, TimeUnit.SECONDS);

        // assert
        verify(koalaDhtStorage, never()).get(any(PId.class), any(Continuation.class));
        verify(koalaDhtStorage).put(eq(id), eq(entityToWrite), isA(Continuation.class));
        assertEquals(entityToWrite, dhtWriter.getValueWritten());
        assertEquals(entityToWrite, valueWritten);
    }

    @Test
    public void shouldPassExceptionBackOnAsyncWrite() throws InterruptedException {
        // setup
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                continuation.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(koalaDhtStorage).put(eq(id), isA(PiEntity.class), isA(Continuation.class));

        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtWriter.put(id, entityToWrite, new Continuation<PiEntity, Exception>() {
            @Override
            public void receiveException(Exception e) {
                cdl.countDown();
            }

            @Override
            public void receiveResult(PiEntity result) {
            }
        });
        cdl.await(5, TimeUnit.SECONDS);

        // assert
        verify(koalaDhtStorage, never()).get(any(PId.class), any(Continuation.class));
        verify(koalaDhtStorage).put(eq(id), eq(entityToWrite), isA(Continuation.class));
        assertEquals("oops", dhtWriter.getException().getMessage());
        assertEquals(null, dhtWriter.getValueWritten());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowTwoWritesWithoutResolver() {
        // act
        dhtWriter.put(id, entityToWrite);
        dhtWriter.put(id, entityToWrite);
    }
}

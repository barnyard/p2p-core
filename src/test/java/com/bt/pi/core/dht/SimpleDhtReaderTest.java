package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
public class SimpleDhtReaderTest {
    private SimpleDhtReader dhtReader;
    private KoalaDHTStorage koalaDhtStorage;
    private PId id;
    private PiEntity piEntity;
    private PiEntity anyPiEntity;
    private ThreadPoolTaskExecutor executor;

    @Before
    public void before() {
        id = new PiId("00000000000000000000000000", 0);
        piEntity = mock(PiEntity.class);
        anyPiEntity = mock(PiEntity.class);
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        koalaDhtStorage = mock(KoalaDHTStorage.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(anyPiEntity);
                return null;
            }
        }).when(koalaDhtStorage).getAny(eq(id), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(piEntity);
                return null;
            }
        }).when(koalaDhtStorage).get(eq(id), isA(Continuation.class));
        dhtReader = new SimpleDhtReader(executor, koalaDhtStorage);
    }

    @Test
    public void shouldGetContent() {
        // act
        PiEntity res = dhtReader.get(id);

        // assert
        assertEquals(piEntity, res);
    }

    @Test
    public void shouldGetAnyContent() {
        // act
        PiEntity res = dhtReader.getAny(id);

        // assert
        assertEquals(anyPiEntity, res);
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
        }).when(koalaDhtStorage).get(eq(id), isA(Continuation.class));

        // act
        dhtReader.get(id);
    }

    @Test
    public void shouldBeAbleToDoAsyncUnconditionalRead() throws InterruptedException {
        // setup
        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtReader.getAsync(id, new Continuation<PiEntity, Exception>() {
            @Override
            public void receiveException(Exception e) {
            }

            @Override
            public void receiveResult(PiEntity result) {
                cdl.countDown();
            }
        });
        cdl.await(5, TimeUnit.SECONDS);

        // assert
        assertEquals(piEntity, dhtReader.getResult());
    }

    @Test
    public void shouldPassExceptionBackOnAsyncWrite() throws InterruptedException {
        // setup
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(koalaDhtStorage).get(eq(id), isA(Continuation.class));

        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtReader.getAsync(id, new Continuation<PiEntity, Exception>() {
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
        assertEquals("oops", dhtReader.getException().getMessage());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowTwoWritesWithoutResolver() {
        // act
        dhtReader.get(id);
        dhtReader.getAsync(id, mock(Continuation.class));
    }
}

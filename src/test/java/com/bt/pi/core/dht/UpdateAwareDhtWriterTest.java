package com.bt.pi.core.dht;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import rice.Continuation;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.exception.KoalaContentVersionMismatchException;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.past.KoalaDHTStorage;

@SuppressWarnings("unchecked")
public class UpdateAwareDhtWriterTest {
    private UpdateAwareDhtWriter dhtWriter;
    private KoalaDHTStorage koalaDhtStorage;
    private PId writeSucceedsId;
    private PId writeFailsId;
    private PId versionMismatchOnFirstWriteId;
    private PId writeFailsFollowingVersionMismatchId;
    private PId writeToNullEntityId;
    private final AtomicInteger numCalls = new AtomicInteger(0);
    private PiEntity existingEntity;
    private PiEntity entityToWrite;
    private PiEntity valueWritten;
    private ThreadPoolTaskExecutor executor;

    @Before
    public void before() {
        writeSucceedsId = new PiId("0000000000000000000000000000000000000000", 0);
        writeFailsId = new PiId("1111111111111111111111111111111111111111", 0);
        versionMismatchOnFirstWriteId = new PiId("2222222222222222222222222222222222222222", 0);
        writeFailsFollowingVersionMismatchId = new PiId("3333333333333333333333333333333333333333", 0);
        writeToNullEntityId = new PiId("4444444444444444444444444444444444444444", 0);

        existingEntity = mock(PiEntity.class);
        entityToWrite = mock(PiEntity.class);
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        numCalls.set(0);

        List<PId> getReturnsExistingEntity = new ArrayList<PId>();
        getReturnsExistingEntity.add(writeFailsFollowingVersionMismatchId);
        getReturnsExistingEntity.add(writeSucceedsId);
        getReturnsExistingEntity.add(writeFailsId);
        getReturnsExistingEntity.add(versionMismatchOnFirstWriteId);

        koalaDhtStorage = mock(KoalaDHTStorage.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(existingEntity);
                return null;
            }
        }).when(koalaDhtStorage).get(not(eq(writeToNullEntityId)), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(null);
                return null;
            }
        }).when(koalaDhtStorage).get(eq(writeToNullEntityId), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                continuation.receiveResult(new Boolean[] { true });
                return null;
            }
        }).when(koalaDhtStorage).put(or(eq(writeSucceedsId), eq(writeToNullEntityId)), isA(PiEntity.class), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                continuation.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(koalaDhtStorage).put(eq(writeFailsId), isA(PiEntity.class), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                int callNum = numCalls.incrementAndGet();
                if (callNum <= 1) {
                    continuation.receiveException(new KoalaContentVersionMismatchException("oops"));
                } else {
                    continuation.receiveResult(new Boolean[] { true });
                }
                return null;
            }
        }).when(koalaDhtStorage).put(eq(versionMismatchOnFirstWriteId), isA(PiEntity.class), isA(Continuation.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[2];
                int callNum = numCalls.incrementAndGet();
                if (callNum <= 1) {
                    continuation.receiveException(new KoalaContentVersionMismatchException("oops"));
                } else {
                    continuation.receiveException(new RuntimeException("oops"));
                }
                return null;
            }
        }).when(koalaDhtStorage).put(eq(writeFailsFollowingVersionMismatchId), isA(PiEntity.class), isA(Continuation.class));

        dhtWriter = new UpdateAwareDhtWriter(executor, koalaDhtStorage);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowTwoWritesOneWithOneWithoutResolver() {
        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, mock(UpdateResolver.class));
        dhtWriter.put(writeSucceedsId, entityToWrite);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowTwoWritesWithResolver() {
        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, mock(UpdateResolver.class));
        dhtWriter.update(writeSucceedsId, entityToWrite, mock(UpdateResolver.class));
    }

    @Test
    public void shouldWriteOriginalObjectWhenReturnedByResolver() {
        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return entityToWrite;
            }
        });

        // assert
        verify(koalaDhtStorage).put(eq(writeSucceedsId), eq(entityToWrite), isA(Continuation.class));
        assertEquals(entityToWrite, dhtWriter.getValueWritten());
    }

    @Test
    public void shouldWriteDifferentObjectWhenReturnedByResolver() {
        // setup
        final PiEntity newEntity = mock(PiEntity.class);

        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return newEntity;
            }
        });

        // assert
        verify(koalaDhtStorage).put(eq(writeSucceedsId), eq(newEntity), isA(Continuation.class));
        assertEquals(newEntity, dhtWriter.getValueWritten());
    }

    @Test
    public void shouldNotWriteWhenNullReturnedByResolver() {
        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return null;
            }
        });

        // assert
        verify(koalaDhtStorage).get(eq(writeSucceedsId), isA(Continuation.class));
        verify(koalaDhtStorage, never()).put(isA(PId.class), isA(PiEntity.class), isA(Continuation.class));
        assertEquals(null, dhtWriter.getValueWritten());
    }

    @Test
    public void shouldBeAbleToDoAsyncWriteWithUpdateResolution() throws InterruptedException {
        // setup
        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, new UpdateResolvingContinuation<PiEntity, Exception>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return entityToWrite;
            }

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
        verify(koalaDhtStorage).put(eq(writeSucceedsId), eq(entityToWrite), isA(Continuation.class));
        assertEquals(entityToWrite, dhtWriter.getValueWritten());
        assertEquals(entityToWrite, valueWritten);
    }

    @Test
    public void shouldGetNullAsResultWhenUpdateResolutionDecidesNotToWrite() throws InterruptedException {
        // setup
        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtWriter.update(writeSucceedsId, entityToWrite, new UpdateResolvingContinuation<PiEntity, Exception>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return null;
            }

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
        verify(koalaDhtStorage, never()).put(eq(writeSucceedsId), isA(PiEntity.class), isA(Continuation.class));
        assertEquals(null, dhtWriter.getValueWritten());
        assertEquals(null, valueWritten);
    }

    @Test
    public void shouldBeAbleToTrapExceptionOnAsyncWrite() throws InterruptedException {
        // setup
        final CountDownLatch cdl = new CountDownLatch(1);

        // act
        dhtWriter.update(writeFailsId, entityToWrite, new UpdateResolvingContinuation<PiEntity, Exception>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return entityToWrite;
            }

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
        verify(koalaDhtStorage).put(eq(writeFailsId), eq(entityToWrite), isA(Continuation.class));
        assertEquals("oops", dhtWriter.getException().getMessage());
        assertEquals(null, dhtWriter.getValueWritten());
    }

    @Test
    public void shouldRetryOnVersionMismatch() {
        // act
        dhtWriter.update(versionMismatchOnFirstWriteId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return requestedEntity;
            }
        });

        // assert
        verify(koalaDhtStorage, times(2)).put(eq(versionMismatchOnFirstWriteId), eq(entityToWrite), isA(Continuation.class));
        assertEquals(entityToWrite, dhtWriter.getValueWritten());
    }

    @Test
    public void shouldBeAbleToAbandonWriteFollowingVersionMismatch() {
        // setup
        final AtomicInteger ai = new AtomicInteger();

        // act
        dhtWriter.update(versionMismatchOnFirstWriteId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                int writeNum = ai.incrementAndGet();
                if (writeNum == 1)
                    return requestedEntity;
                else
                    return null;
            }
        });

        // assert
        verify(koalaDhtStorage, times(1)).put(eq(versionMismatchOnFirstWriteId), eq(entityToWrite), isA(Continuation.class));
        assertNull(dhtWriter.getException());
        assertNull(dhtWriter.getValueWritten());
    }

    @Test(expected = RuntimeException.class)
    public void shouldHandleExceptionOccurringOnWriteFollowingVersionMismatch() {
        // act
        dhtWriter.update(writeFailsFollowingVersionMismatchId, entityToWrite, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return requestedEntity;
            }
        });
    }

    @Test
    public void shouldThrowWhenDoingAWriteWithRetriesLeftAtZero() {
        // setup
        dhtWriter.setMaxNumVersionMismatchRetries(0);

        // act
        dhtWriter.handleExceptionOnWrite(new KoalaContentVersionMismatchException("oops"), new Continuation<Boolean[], Exception>() {
            @Override
            public void receiveException(Exception arg0) {
            }

            @Override
            public void receiveResult(Boolean[] arg0) {
            }
        });

        // assert
        assertTrue(dhtWriter.getException() instanceof DhtOperationMaximumRetriesExceededException);
    }

    @Test
    public void shouldGetWaitIntervalWithinExpectedRange() {
        // setup
        dhtWriter.setMaxNumVersionMismatchRetries(2);
        dhtWriter.setVersionMismatchRetriesLeft(1);

        // act
        long res = dhtWriter.getWaitTimeMillis();

        // assert
        assertTrue(String.format("%d should have been > %d", res, 2 * UpdateAwareDhtWriter.WAIT_FACTOR_MILLIS), res >= (2 * UpdateAwareDhtWriter.WAIT_FACTOR_MILLIS));
        assertTrue(String.format("%d should have been  %d", res, 2 * 2 * UpdateAwareDhtWriter.WAIT_FACTOR_MILLIS), res <= 2 * 2 * UpdateAwareDhtWriter.WAIT_FACTOR_MILLIS);
    }

    @Test
    public void shouldNotWriteTheEntityIfItExists() {
        // setup

        // act
        boolean result = dhtWriter.writeIfAbsent(writeSucceedsId, entityToWrite);

        // assert
        assertFalse("Nothing should have been written", result);
        assertNull(dhtWriter.getValueWritten());
    }

    static class DeletableEntity extends PiEntityBase implements Deletable {
        private boolean deleted;

        public DeletableEntity() {
        }

        public DeletableEntity(boolean b) {
            this.deleted = b;
        }

        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return "abc123";
        }

        @Override
        public boolean isDeleted() {
            return this.deleted;
        }

        @Override
        public void setDeleted(boolean b) {
            deleted = b;
        }

        @Override
        public String getUriScheme() {
            return getClass().getSimpleName();
        }
    }

    @Test
    public void shouldWriteTheEntityIfItExistsButIsDeletableAndDeleted() {
        // setup
        int currentVersion = 10;
        DeletableEntity d = new DeletableEntity();
        d.setDeleted(false);

        existingEntity = new DeletableEntity(true);
        existingEntity.setVersion(currentVersion);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(existingEntity);
                return null;
            }
        }).when(koalaDhtStorage).get(eq(writeSucceedsId), isA(Continuation.class));

        // act
        boolean result = dhtWriter.writeIfAbsent(writeSucceedsId, d);

        // assert
        assertTrue("Should have written the entity", result);
        assertEquals(currentVersion + 1, dhtWriter.getValueWritten().getVersion());
        assertNotNull(dhtWriter.getValueWritten());
    }

    @Test
    public void shouldNotWriteTheEntityIfItExistsButIsDeletableAndNotDeleted() {
        // setup
        existingEntity = new DeletableEntity(false);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation continuation = (Continuation) invocation.getArguments()[1];
                continuation.receiveResult(existingEntity);
                return null;
            }
        }).when(koalaDhtStorage).get(eq(writeSucceedsId), isA(Continuation.class));

        // act
        boolean result = dhtWriter.writeIfAbsent(writeSucceedsId, entityToWrite);

        // assert
        assertFalse("Nothing should have been written", result);
        assertNull(dhtWriter.getValueWritten());
    }

    @Test
    public void shouldWriteTheEntityAsNothingExists() {

        // act
        boolean result = dhtWriter.writeIfAbsent(writeToNullEntityId, entityToWrite);

        // assert
        assertTrue("Should have written the entity", result);
        assertNotNull(dhtWriter.getValueWritten());
    }
}
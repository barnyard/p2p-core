package com.bt.pi.core.application.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.Continuation;
import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategy;
import com.bt.pi.core.application.resource.watched.SharedResourceWatchingStrategyFactory;
import com.bt.pi.core.application.watcher.service.WatcherService;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.testing.TestFriendlyContinuation;
import com.bt.pi.core.testing.ThrowingContinuationAnswer;

public class ConsumedDhtResourceRegistryTest {
    private ConsumedDhtResourceRegistry consumedDhtResourceRegistry;
    private DhtCache dhtCache;
    private PId zeroId;
    private PId oneId;
    private PId unknownId;
    private PiEntity firstEntity;
    private PiEntity secondEntity;
    private CountDownLatch dhtReaderLatch;
    private WatcherService watcherService;
    @SuppressWarnings("unchecked")
    private SharedResourceWatchingStrategy bespokeSharedResourceWatchingStrategy;
    private Runnable bespokeRefreshRunner;
    private Runnable bespokeConsumerWatcher;
    private SharedResourceWatchingStrategyFactory sharedResourceWatchingStrategyFactory;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        dhtReaderLatch = new CountDownLatch(1);
        firstEntity = mock(PiEntity.class);
        secondEntity = mock(PiEntity.class);
        bespokeRefreshRunner = mock(Runnable.class);
        bespokeConsumerWatcher = mock(Runnable.class);

        bespokeSharedResourceWatchingStrategy = mock(SharedResourceWatchingStrategy.class);
        when(bespokeSharedResourceWatchingStrategy.getSharedResourceRefreshRunner(any(Id.class))).thenReturn(bespokeRefreshRunner);
        when(bespokeSharedResourceWatchingStrategy.getConsumerWatcher(any(Id.class), any(String.class))).thenReturn(bespokeConsumerWatcher);

        sharedResourceWatchingStrategyFactory = mock(SharedResourceWatchingStrategyFactory.class);
        when(sharedResourceWatchingStrategyFactory.createWatchingStrategy(PiEntity.class)).thenReturn(bespokeSharedResourceWatchingStrategy);
        zeroId = new PiId("0000000000000000000000000000000000000000", 0);
        oneId = new PiId("1111111111111111111111111111111111111111", 0);
        unknownId = new PiId("9999999999999999999999999999999999999999", 0);

        dhtCache = mock(DhtCache.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (zeroId.equals(invocation.getArguments()[0]))
                    ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(firstEntity);
                else
                    ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveException(new RuntimeException("not found"));
                dhtReaderLatch.countDown();
                return null;
            }
        }).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));

        watcherService = mock(WatcherService.class);

        consumedDhtResourceRegistry = new ConsumedDhtResourceRegistry();
        consumedDhtResourceRegistry.setDhtCache(dhtCache);
        consumedDhtResourceRegistry.setWatcherService(watcherService);
        consumedDhtResourceRegistry.setSharedResourceWatchingStrategyFactory(sharedResourceWatchingStrategyFactory);
    }

    private void register(String consumer) throws InterruptedException {
        register(zeroId, consumer);
    }

    private void register(PId id, String consumer) throws InterruptedException {
        TestFriendlyContinuation<Boolean> alphaContinuation = new TestFriendlyContinuation<Boolean>();
        consumedDhtResourceRegistry.registerConsumer(id, consumer, PiEntity.class, alphaContinuation);
        assertTrue(alphaContinuation.completedLatch.await(5, TimeUnit.SECONDS));
    }

    private void setupDhtCacheReadExpectations(final PiEntity res) {
        setupDhtCacheReadExpectations(zeroId, res);
    }

    @SuppressWarnings("unchecked")
    private void setupDhtCacheReadExpectations(final PId id, final PiEntity res) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation<PiEntity, Exception> continuation = (Continuation<PiEntity, Exception>) invocation.getArguments()[1];
                continuation.receiveResult(res);
                dhtReaderLatch.countDown();
                return null;
            }
        }).when(dhtCache).getReadThrough(eq(id), isA(GenericContinuation.class));
    }

    @SuppressWarnings("unchecked")
    private void setupDhtCacheWriteExpectations(final PiEntity res) {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiEntity requestedEntity = (PiEntity) invocation.getArguments()[1];
                Continuation<PiEntity, Exception> continuation = (Continuation<PiEntity, Exception>) invocation.getArguments()[2];
                PiEntity updatedEntity = ((UpdateResolver<PiEntity>) invocation.getArguments()[2]).update(requestedEntity, requestedEntity);
                continuation.receiveResult(updatedEntity);

                return null;
            }
        }).when(dhtCache).update(eq(zeroId), (PiEntity) anyObject(), isA(UpdateResolvingContinuation.class));
    }

    @Test
    public void shouldCreateNewResourceOnRegister() throws Exception {
        // setup
        TestFriendlyContinuation<Boolean> continuation = new TestFriendlyContinuation<Boolean>();

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(continuation.lastResult);
        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
    }

    @Test
    public void shouldAddRefreshRunnerAndConsumerWatcherFromDefaultWatcherStrategyOnRegister() throws Exception {
        // setup
        TestFriendlyContinuation<Boolean> continuation = new TestFriendlyContinuation<Boolean>();

        when(bespokeSharedResourceWatchingStrategy.getInitialResourceRefreshIntervalMillis()).thenReturn(300000L);
        when(bespokeSharedResourceWatchingStrategy.getRepeatingResourceRefreshIntervalMillis()).thenReturn(600000L);

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(continuation.lastResult);
        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-resource-refresh-runner"), isA(Runnable.class), eq(300000L), eq(600000L));
        verify(watcherService, never()).replaceTask(eq("alpha-consumer-watcher"), isA(Runnable.class), eq(0L), eq(0L));
    }

    @Test
    public void shouldBeAbleToSpecifyBespokeWatcherStrategyOnRegister() throws Exception {
        // setup
        TestFriendlyContinuation<Boolean> continuation = new TestFriendlyContinuation<Boolean>();

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(continuation.lastResult);
        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-resource-refresh-runner"), eq(bespokeRefreshRunner), eq(0L), eq(0L));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-alpha-consumer-watcher"), eq(bespokeConsumerWatcher), eq(0L), eq(0L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBeAbleToRegisterASecondConsumer() throws Exception {
        // setup
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, mock(Continuation.class));

        TestFriendlyContinuation<Boolean> secondContinuation = new TestFriendlyContinuation<Boolean>();

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "beta", PiEntity.class, secondContinuation);

        // assert
        assertTrue(dhtReaderLatch.await(5, TimeUnit.SECONDS));
        assertTrue(secondContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertFalse(secondContinuation.lastResult);
        verify(dhtCache, times(1)).getReadThrough(eq(zeroId), isA(GenericContinuation.class));
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCorrectlySetUpResourceAndConsumerWatchersWhenRegisteringTwoResourceConsumers() throws Exception {
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, mock(Continuation.class));

        TestFriendlyContinuation<Boolean> secondContinuation = new TestFriendlyContinuation<Boolean>();

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "beta", PiEntity.class, secondContinuation);

        // assert
        assertTrue(dhtReaderLatch.await(5, TimeUnit.SECONDS));
        assertTrue(secondContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-resource-refresh-runner"), eq(bespokeRefreshRunner), eq(0L), eq(0L));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-alpha-consumer-watcher"), eq(bespokeConsumerWatcher), eq(0L), eq(0L));
        verify(watcherService).replaceTask(eq(zeroId.toStringFull() + "-beta-consumer-watcher"), eq(bespokeConsumerWatcher), eq(0L), eq(0L));
    }

    @Test
    public void shouldRegisterWhenUnableToGetResourceFromDhtButEntityWillBeNull() throws Exception {
        // setup
        TestFriendlyContinuation<Boolean> continuation = new TestFriendlyContinuation<Boolean>();

        // act
        consumedDhtResourceRegistry.registerConsumer(oneId, "beta", PiEntity.class, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertNull(continuation.lastResult);
        assertNull(consumedDhtResourceRegistry.getCachedEntity(oneId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDelegateRegisterExceptionToCallingContinuation() throws InterruptedException {
        // setup
        TestFriendlyContinuation<Boolean> continuation = new TestFriendlyContinuation<Boolean>();

        Exception e = new Exception("oops");
        ThrowingContinuationAnswer answer = new ThrowingContinuationAnswer(e);
        doAnswer(answer).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));

        // act
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(e, continuation.lastException);
    }

    @Test
    public void shouldNoOpWhenDeregisteringUnknownResource() throws InterruptedException {
        // act
        boolean res = consumedDhtResourceRegistry.deregisterConsumer(unknownId, "alpha");

        // assert
        assertFalse(res);
        assertEquals(null, consumedDhtResourceRegistry.getCachedEntity(unknownId));
    }

    @Test
    public void shouldBeAbleToDeregisterAResource() throws InterruptedException {
        // setup
        register("alpha");

        // act
        boolean res = consumedDhtResourceRegistry.deregisterConsumer(zeroId, "alpha");

        // assert
        assertTrue(res);
        assertEquals(null, consumedDhtResourceRegistry.getCachedEntity(zeroId));
    }

    @Test
    public void shouldRemoveTasksFromWatcherOnDeregister() throws Exception {
        // setup
        register("alpha");

        // act
        consumedDhtResourceRegistry.deregisterConsumer(zeroId, "alpha");

        // assert
        verify(watcherService).removeTask(zeroId.toStringFull() + "-resource-refresh-runner");
        verify(watcherService).removeTask(zeroId.toStringFull() + "-alpha-consumer-watcher");
    }

    @Test
    public void shouldNoOpWhenDeregisteringAResourceForAnUnknownConsumer() throws InterruptedException {
        // setup
        register("alpha");

        // act
        boolean res = consumedDhtResourceRegistry.deregisterConsumer(zeroId, "beta");

        // assert
        assertFalse(res);
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
    }

    @Test
    public void shouldLeaveResourceWhenDeregisteringOneOfManyConsumers() throws InterruptedException {
        // setup
        register("alpha");
        register("beta");

        // act
        boolean res = consumedDhtResourceRegistry.deregisterConsumer(zeroId, "alpha");

        // assert
        assertFalse(res);
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
    }

    @Test
    public void shouldRemoveConsumerWatcherWhenDeregisteringOneOfManyConsumers() throws InterruptedException {
        // setup
        register("alpha");
        register("beta");

        // act
        consumedDhtResourceRegistry.deregisterConsumer(zeroId, "alpha");

        // assert
        verify(watcherService).removeTask(zeroId.toStringFull() + "-alpha-consumer-watcher");
        verify(watcherService, never()).removeTask(zeroId.toStringFull() + "-resource-refresh-runner");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBeAbleToRefreshAResource() throws InterruptedException {
        // setup
        register("alpha");
        setupDhtCacheReadExpectations(secondEntity);

        TestFriendlyContinuation<PiEntity> refreshContinuation = new TestFriendlyContinuation<PiEntity>();

        // act
        consumedDhtResourceRegistry.refresh(zeroId, refreshContinuation);

        // assert
        assertTrue(refreshContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        verify(dhtCache, times(2)).getReadThrough(eq(zeroId), isA(GenericContinuation.class));
        assertEquals(secondEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
        assertEquals(secondEntity, refreshContinuation.lastResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldNoOpWhenRefreshingAnUnknownResource() {
        // act
        consumedDhtResourceRegistry.refresh(oneId, mock(Continuation.class));

        // assert
        assertEquals(null, consumedDhtResourceRegistry.getCachedEntity(oneId));
    }

    @Test
    public void shouldNoOpWhenRefreshDoesNotReturnAResultFromDht() throws InterruptedException {
        // setup
        register("alpha");
        setupDhtCacheReadExpectations(null);

        TestFriendlyContinuation<PiEntity> refreshContinuation = new TestFriendlyContinuation<PiEntity>();

        // act
        consumedDhtResourceRegistry.refresh(zeroId, refreshContinuation);

        // assert
        assertTrue(refreshContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
        assertEquals(null, refreshContinuation.lastResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDelegateRefreshExceptionToCallingContinuation() throws InterruptedException {
        // setup
        register("alpha");
        TestFriendlyContinuation<PiEntity> continuation = new TestFriendlyContinuation<PiEntity>();

        Exception e = new Exception("oops");
        ThrowingContinuationAnswer answer = new ThrowingContinuationAnswer(e);
        doAnswer(answer).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));

        // act
        consumedDhtResourceRegistry.refresh(zeroId, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(e, continuation.lastException);
    }

    @Test
    public void shouldCacheUpdatedEntity() throws InterruptedException {
        // setup
        register("alpha");
        setupDhtCacheWriteExpectations(secondEntity);

        TestFriendlyContinuation<PiEntity> updateContinuation = new TestFriendlyContinuation<PiEntity>();

        // act
        consumedDhtResourceRegistry.update(zeroId, secondEntity, updateContinuation);

        // assert
        assertTrue(updateContinuation.updatedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(updateContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(secondEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
        assertEquals(secondEntity, updateContinuation.lastResult);
    }

    @Test
    public void shouldCacheUpdatedEntityUsingOverload() throws InterruptedException {
        // setup
        register("alpha");
        setupDhtCacheWriteExpectations(secondEntity);

        TestFriendlyContinuation<PiEntity> updateContinuation = new TestFriendlyContinuation<PiEntity>();

        // act
        consumedDhtResourceRegistry.update(zeroId, updateContinuation);

        // assert
        assertTrue(updateContinuation.updatedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(updateContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
        assertEquals(null, updateContinuation.lastResult);
    }

    @Test
    public void shouldNoOpWhenNoUpdateHappens() throws InterruptedException {
        // setup
        register("alpha");
        setupDhtCacheWriteExpectations(secondEntity);

        TestFriendlyContinuation<PiEntity> updateContinuation = new TestFriendlyContinuation<PiEntity>();

        // act
        consumedDhtResourceRegistry.update(zeroId, updateContinuation);

        // assert
        assertTrue(updateContinuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(firstEntity, consumedDhtResourceRegistry.getCachedEntity(zeroId));
        assertEquals(null, updateContinuation.lastResult);
    }

    @Test
    public void shouldNoOpWhenUpdateResourceUnknownHappens() {
        // act
        consumedDhtResourceRegistry.update(oneId, secondEntity, new TestFriendlyContinuation<PiEntity>());

        // assert
        assertEquals(null, consumedDhtResourceRegistry.getCachedEntity(oneId));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDelegateUpdateExceptionToCallingContinuation() throws InterruptedException {
        // setup
        register("alpha");
        TestFriendlyContinuation<PiEntity> continuation = new TestFriendlyContinuation<PiEntity>();

        Exception e = new Exception("oops");
        ThrowingContinuationAnswer answer = new ThrowingContinuationAnswer(e);
        doAnswer(answer).when(dhtCache).update(isA(PId.class), (PiEntity) anyObject(), isA(UpdateResolvingContinuation.class));

        // act
        consumedDhtResourceRegistry.update(zeroId, continuation);

        // assert
        assertTrue(continuation.completedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(null, continuation.lastResult);
        assertEquals(e, continuation.lastException);
    }

    class ApplePiEntity extends PiEntityBase {
        @Override
        public String getType() {
            return this.getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return "ap";
        }
    }

    class CherryPiEntity extends PiEntityBase {
        @Override
        public String getType() {
            return this.getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return "cp";
        }
    }

    @SuppressWarnings("unchecked")
    private void setupFruitEntityDhtExpectation(final ApplePiEntity firstApplePi, final ApplePiEntity secondApplePi) {
        dhtReaderLatch = new CountDownLatch(3);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                if (zeroId.equals(invocation.getArguments()[0]))
                    ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(firstApplePi);
                else if (oneId.equals(invocation.getArguments()[0]))
                    ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(new CherryPiEntity());
                else
                    ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(secondApplePi);
                dhtReaderLatch.countDown();
                return null;
            }
        }).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));
    }

    @Test
    public void shouldBeAbleToGetAllResourcesOfACertainType() throws Exception {
        // setup
        final ApplePiEntity firstApplePi = new ApplePiEntity();
        final ApplePiEntity secondApplePi = new ApplePiEntity();

        setupFruitEntityDhtExpectation(firstApplePi, secondApplePi);

        register(zeroId, "alpha");
        register(oneId, "alpha");
        register(unknownId, "alpha");
        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));

        // act
        List<ApplePiEntity> res = consumedDhtResourceRegistry.getByType(ApplePiEntity.class);

        // assert
        assertEquals(2, res.size());
        assertTrue(res.contains(firstApplePi));
        assertTrue(res.contains(secondApplePi));
    }

    @Test
    public void shouldDoNothingWhenClearingNonExistentResourceById() throws Exception {
        // act
        consumedDhtResourceRegistry.clearResource(mock(PId.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToClearSingleResourceById() throws Exception {
        // setup
        final ApplePiEntity firstApplePi = new ApplePiEntity();

        dhtReaderLatch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(firstApplePi);
                dhtReaderLatch.countDown();
                return null;
            }
        }).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));

        register(zeroId, "alpha");

        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));

        // act
        consumedDhtResourceRegistry.clearResource(zeroId);

        // assert
        assertEquals(0, consumedDhtResourceRegistry.getResourceMap().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBeAbleToClearSingleResourceOfACertainType() throws Exception {
        // setup
        final ApplePiEntity firstApplePi = new ApplePiEntity();

        dhtReaderLatch = new CountDownLatch(1);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Continuation<PiEntity, Exception>) invocation.getArguments()[1]).receiveResult(firstApplePi);
                dhtReaderLatch.countDown();
                return null;
            }
        }).when(dhtCache).getReadThrough(isA(PId.class), isA(GenericContinuation.class));

        register(zeroId, "alpha");

        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));

        // act
        consumedDhtResourceRegistry.clearAll(ApplePiEntity.class);

        // assert
        assertEquals(0, consumedDhtResourceRegistry.getResourceMap().size());
    }

    @Test
    public void shouldBeAbleToClearMultipleResourcesOfACertainTypeWithMultipleConsumers() throws Exception {
        // setup
        final ApplePiEntity firstApplePi = new ApplePiEntity();
        final ApplePiEntity secondApplePi = new ApplePiEntity();

        setupFruitEntityDhtExpectation(firstApplePi, secondApplePi);

        register(zeroId, "alpha");
        register(zeroId, "beta");
        register(oneId, "alpha");
        register(unknownId, "alpha");

        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));

        // act
        consumedDhtResourceRegistry.clearAll(ApplePiEntity.class);

        // assert
        assertEquals(1, consumedDhtResourceRegistry.getResourceMap().size());
        assertTrue(consumedDhtResourceRegistry.getCachedEntity(oneId) instanceof CherryPiEntity);
    }

    @Test
    public void shouldGetNothingWhenGettingUnknownResourcesByType() throws Exception {
        // setup
        register(oneId, "alpha");
        setupDhtCacheReadExpectations(oneId, new CherryPiEntity());
        assertTrue(dhtReaderLatch.await(1, TimeUnit.SECONDS));

        // act
        List<ApplePiEntity> res = consumedDhtResourceRegistry.getByType(ApplePiEntity.class);

        // assert
        assertEquals(0, res.size());
    }

    @Test
    public void testGetKeyAsString() throws Exception {
        // assert
        assertEquals(zeroId.toStringFull(), consumedDhtResourceRegistry.getKeyAsString(zeroId));
    }

    @Test
    public void shouldHaveGetAllConsumersReturnEmptySetWhenNoConsumers() throws Exception {
        // act
        Set<String> consumers = consumedDhtResourceRegistry.getAllConsumers(zeroId);

        // assert
        assertEquals(0, consumers.size());
    }

    @Test
    public void shouldBeAbleToGetAllConsumersWhenTwoConsumers() throws Exception {
        // setup
        consumedDhtResourceRegistry.registerConsumer(zeroId, "alpha", PiEntity.class, new LoggingContinuation<Boolean>());
        consumedDhtResourceRegistry.registerConsumer(zeroId, "beta", PiEntity.class, new LoggingContinuation<Boolean>());

        // act
        Set<String> consumers = consumedDhtResourceRegistry.getAllConsumers(zeroId);

        // assert
        assertEquals(2, consumers.size());
        assertTrue(consumers.contains("alpha"));
        assertTrue(consumers.contains("beta"));
    }
}

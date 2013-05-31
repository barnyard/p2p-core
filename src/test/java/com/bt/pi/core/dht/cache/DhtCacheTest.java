package com.bt.pi.core.dht.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

import rice.Continuation;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@RunWith(MockitoJUnitRunner.class)
public class DhtCacheTest {
    private DhtCache dhtCache;
    private Cache cache;
    private PId id;
    @Mock
    private PiEntity cachedPiEntity;
    @Mock
    private PiEntity dhtPiEntity;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @SuppressWarnings("unchecked")
    @Mock
    private UpdateResolvingPiContinuation updateResolvingContinuation;
    private TestContinuation<PiEntity> continuation;
    private GenericContinuationAnswer<PiEntity> dhtPiEntityReadAnswer;
    private UpdateResolvingContinuationAnswer dhtPiEntityUpdateAnswer;

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws Exception {
        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheName("unittest");
        ehCacheFactoryBean.afterPropertiesSet();
        this.cache = (Cache) ehCacheFactoryBean.getObject();

        id = mock(PId.class);

        continuation = new TestContinuation<PiEntity>();

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        dhtPiEntityReadAnswer = new GenericContinuationAnswer<PiEntity>(dhtPiEntity);
        doAnswer(dhtPiEntityReadAnswer).when(dhtReader).getAsync(eq(id), isA(Continuation.class));

        dhtPiEntityUpdateAnswer = new UpdateResolvingContinuationAnswer(dhtPiEntity);
        doAnswer(dhtPiEntityUpdateAnswer).when(dhtWriter).update(eq(id), (PiEntity) any(), isA(UpdateResolvingPiContinuation.class));

        when(updateResolvingContinuation.update(isA(PiEntity.class), isA(PiEntity.class))).thenReturn(dhtPiEntity);

        dhtCache = new DhtCache();
        dhtCache.setCache(this.cache);
        dhtCache.setDhtClientFactory(dhtClientFactory);
    }

    private static class TestContinuation<T extends PiEntity> extends PiContinuation<T> {
        private PiEntity lastResult;
        private Throwable lastExecption;

        public PiEntity getResult() {
            return lastResult;
        }

        @Override
        public void handleResult(T result) {
            lastResult = result;
        }

        @Override
        public void handleException(Exception e) {
            lastExecption = e;
        }

        public Object getException() {
            return lastExecption;
        }
    }

    // not sure why I need this!!
    @After
    public void after() {
        this.cache.removeAll();
    }

    @Test
    public void shouldGetFromCacheIfPresent() throws InterruptedException {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));

        // act
        dhtCache.get(id, continuation);

        // assert
        assertEquals(cachedPiEntity, continuation.getResult());
    }

    @Test
    public void shouldTryToGetFromDHTOnCacheMiss() throws InterruptedException {
        // act
        dhtCache.get(id, continuation);

        // assert
        assertEquals(dhtPiEntity, continuation.getResult());
        assertEquals(dhtPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDelegateReadExceptionToCallingContinuation() throws Exception {
        // setup
        final RuntimeException exception = mock(RuntimeException.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation<PiEntity, Exception>) invocation.getArguments()[1];
                c.receiveException(exception);
                return null;
            }
        }).when(dhtReader).getAsync(eq(id), isA(Continuation.class));

        // act
        dhtCache.get(id, continuation);

        // assert
        assertNull(continuation.getResult());
        assertEquals(exception, continuation.getException());
        assertEquals(null, dhtCache.getCache().get(id));
    }

    @Test
    public void shouldReadThroughToDhtEvenIfAlreadyInTheCache() throws Exception {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));
        when(cachedPiEntity.getVersion()).thenReturn(2L);
        when(dhtPiEntity.getVersion()).thenReturn(3L);

        // act
        dhtCache.getReadThrough(id, continuation);

        // assert
        assertEquals(dhtPiEntity, continuation.getResult());
        assertEquals(dhtPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @Test
    public void shouldReadThroughToDhtButUseCachedVersionIfItIsNewer() throws Exception {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));
        when(cachedPiEntity.getVersion()).thenReturn(3L);
        when(dhtPiEntity.getVersion()).thenReturn(2L);

        // act
        dhtCache.getReadThrough(id, continuation);

        // assert
        assertEquals(cachedPiEntity, continuation.getResult());
        assertEquals(cachedPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadThroughToDhtButUseCachedVersionDhtReturnsNull() throws Exception {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));
        dhtPiEntityReadAnswer = new GenericContinuationAnswer<PiEntity>(null);
        doAnswer(dhtPiEntityReadAnswer).when(dhtReader).getAsync(eq(id), isA(Continuation.class));

        // act
        dhtCache.getReadThrough(id, continuation);

        // assert
        assertEquals(cachedPiEntity, continuation.getResult());
        assertEquals(cachedPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateCacheValueOnDhtUpdate() {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));

        // act
        dhtCache.update(id, updateResolvingContinuation);

        // assert
        verify(updateResolvingContinuation).receiveResult(dhtPiEntity);
        assertEquals(dhtPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldLeaveCacheValueOnDhtUpdateIfDhtRecordIsNull() {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));

        dhtPiEntityUpdateAnswer = new UpdateResolvingContinuationAnswer(null);
        doAnswer(dhtPiEntityUpdateAnswer).when(dhtWriter).update(eq(id), (PiEntity) any(), isA(UpdateResolvingContinuation.class));

        when(updateResolvingContinuation.update(isA(PiEntity.class), isA(PiEntity.class))).thenReturn(null);

        // act
        dhtCache.update(id, updateResolvingContinuation);

        // assert
        verify(updateResolvingContinuation).receiveResult(null);
        assertEquals(cachedPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDelegateExceptionOnUpdateBackToCallingContinuation() {
        // setup
        dhtCache.getCache().put(new Element(id, cachedPiEntity));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation<PiEntity, Exception>) invocation.getArguments()[2];
                c.receiveException(new RuntimeException("oops"));
                return null;
            }
        }).when(dhtWriter).update(eq(id), (PiEntity) any(), isA(UpdateResolvingContinuation.class));

        // act
        dhtCache.update(id, updateResolvingContinuation);

        // assert
        verify(updateResolvingContinuation).receiveException(isA(RuntimeException.class));
        assertEquals(cachedPiEntity, dhtCache.getCache().get(id).getObjectValue());
    }
}

package com.bt.pi.core.dht.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.cache.ehcache.EhCacheFactoryBean;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

public class BlockingDhtCacheTest {
    private BlockingDhtCache blockingDhtCache;
    private DhtClientFactory dhtClientFactory;
    private PiEntity piEntity;
    private PId id;
    private BlockingDhtReader blockingReader;
    private BlockingDhtWriter blockingWriter;
    private Cache cache;

    @Before
    public void setUp() throws Exception {
        this.blockingDhtCache = new BlockingDhtCache();

        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheName("unittest");
        ehCacheFactoryBean.afterPropertiesSet();
        this.cache = (Cache) ehCacheFactoryBean.getObject();
        this.blockingDhtCache.setCache(this.cache);
        this.dhtClientFactory = mock(DhtClientFactory.class);
        this.blockingDhtCache.setDhtClientFactory(this.dhtClientFactory);
        this.id = mock(PId.class);
        when(this.id.toStringFull()).thenReturn("" + System.currentTimeMillis());
        this.piEntity = mock(PiEntity.class);
        this.blockingReader = mock(BlockingDhtReader.class);
        when(this.dhtClientFactory.createBlockingReader()).thenReturn(this.blockingReader);
        this.blockingWriter = mock(BlockingDhtWriter.class);
        when(this.dhtClientFactory.createBlockingWriter()).thenReturn(this.blockingWriter);
    }

    // not sure why I need this!!
    @After
    public void after() {
        cache.removeAll();
    }

    @Test
    public void testGetShouldReturnCachedEntity() {
        // setup
        this.blockingDhtCache.getCache().put(new Element(id, piEntity));

        // act
        PiEntity result = this.blockingDhtCache.get(id);

        // assert
        assertEquals(this.piEntity, result);
    }

    @Test
    public void testGetShouldCallDhtOnCacheMiss() {
        // setup
        when(this.blockingReader.get(id)).thenReturn(piEntity);

        // act
        PiEntity result = this.blockingDhtCache.get(id);

        // assert
        assertEquals(this.piEntity, result);
        verify(this.blockingReader).get(id);
    }

    @Test
    public void testGetShouldCacheFromDhtOnCacheMiss() {
        // setup
        when(this.blockingReader.get(id)).thenReturn(piEntity);

        // act
        PiEntity result = this.blockingDhtCache.get(id);

        // assert
        assertEquals(this.piEntity, result);
        assertTrue(this.blockingDhtCache.getCache().isKeyInCache(id));
        assertEquals(this.piEntity, this.blockingDhtCache.getCache().get(id).getObjectValue());
    }

    @Test
    public void testGetReadThrough() {
        // setup
        cache.removeAll();
        when(this.blockingReader.get(id)).thenReturn(piEntity);

        // act
        PiEntity result = this.blockingDhtCache.getReadThrough(id);

        // assert
        assertEquals(this.piEntity, result);
        verify(this.blockingReader).get(id);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateShouldPopulateCache() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                resolver.update(piEntity, piEntity);
                return null;
            }
        }).when(this.blockingWriter).update(eq(id), eq(this.piEntity), isA(UpdateResolver.class));
        when(this.blockingWriter.getValueWritten()).thenReturn(piEntity);

        // act
        this.blockingDhtCache.update(id, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return existingEntity;
            }
        });

        // assert
        assertEquals(piEntity, this.blockingDhtCache.getCache().get(id).getObjectValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateShouldUpdateCache() throws Exception {
        // setup
        this.blockingDhtCache.getCache().put(new Element(id, mock(PiEntity.class)));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver resolver = (UpdateResolver) invocation.getArguments()[2];
                resolver.update(piEntity, piEntity);
                return null;
            }
        }).when(this.blockingWriter).update(eq(id), eq(this.piEntity), isA(UpdateResolver.class));
        when(this.blockingWriter.getValueWritten()).thenReturn(piEntity);

        // act
        this.blockingDhtCache.update(id, new UpdateResolver<PiEntity>() {
            @Override
            public PiEntity update(PiEntity existingEntity, PiEntity requestedEntity) {
                return existingEntity;
            }
        });

        // assert
        assertEquals(piEntity, this.blockingDhtCache.getCache().get(id).getObjectValue());
    }

    @Test
    public void testWriteIfAbsentShouldUpdateCache() throws Exception {
        // setup
        when(this.blockingWriter.writeIfAbsent(id, piEntity)).thenReturn(true);
        when(this.blockingWriter.getValueWritten()).thenReturn(piEntity);

        // act
        boolean result = this.blockingDhtCache.writeIfAbsent(id, piEntity);

        // assert
        assertTrue(result);
        assertEquals(piEntity, this.blockingDhtCache.getCache().get(id).getObjectValue());
    }

    @Ignore("because this is just testing ehcache which hopefully works anyhow")
    @Test
    public void testThatCacheDoesExpire() throws Exception {
        // setup
        int ttl = 2;
        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheName("unittest1");
        ehCacheFactoryBean.setTimeToIdle(ttl);
        ehCacheFactoryBean.setTimeToLive(ttl);
        ehCacheFactoryBean.afterPropertiesSet();
        this.cache = (Cache) ehCacheFactoryBean.getObject();
        this.blockingDhtCache.setCache(this.cache);

        when(this.blockingReader.get(id)).thenReturn(piEntity);

        // act
        PiEntity result = this.blockingDhtCache.get(id);
        assertEquals(this.piEntity, result);
        Thread.sleep(ttl * 2 * 1000);

        result = this.blockingDhtCache.get(id);
        assertEquals(this.piEntity, result);

        // assert
        verify(this.blockingReader, times(2)).get(id);
    }
}

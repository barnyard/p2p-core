package com.bt.pi.core.dht;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.core.past.KoalaDHTStorage;

public class DhtClientFactoryTest {
    private DhtClientFactory dhtClientFactory;
    private KoalaDHTStorage koalaDHTStorage;
    private ThreadPoolTaskExecutor executor;

    @Before
    public void before() {
        executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        koalaDHTStorage = mock(KoalaDHTStorage.class);

        this.dhtClientFactory = new SubDhtClientFactory();
        this.dhtClientFactory.setKoalaDhtStorage(koalaDHTStorage);
        this.dhtClientFactory.setExecutor(executor);
    }

    @Test
    public void shouldCreateWriter() {
        // act
        UnconditionalDhtWriter dw = (UnconditionalDhtWriter) dhtClientFactory.createWriter();

        // assert
        assertNotNull(dw);
        assertSame(koalaDHTStorage, dw.getStorage());
    }

    @Test
    public void shouldCreateBlockingWriter() {
        // act
        UnconditionalDhtWriter dw = (UnconditionalDhtWriter) dhtClientFactory.createBlockingWriter();

        // assert
        assertNotNull(dw);
        assertSame(koalaDHTStorage, dw.getStorage());
    }

    @Test
    public void shouldCreateReader() {
        // act
        SimpleDhtReader dr = (SimpleDhtReader) dhtClientFactory.createReader();

        // assert
        assertNotNull(dr);
        assertSame(koalaDHTStorage, dr.getStorage());
    }

    @Test
    public void shouldCreateBlockingReader() {
        // act
        SimpleDhtReader dr = (SimpleDhtReader) dhtClientFactory.createReader();

        // assert
        assertNotNull(dr);
        assertSame(koalaDHTStorage, dr.getStorage());
    }
}

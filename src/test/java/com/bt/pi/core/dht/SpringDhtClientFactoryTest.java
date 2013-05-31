package com.bt.pi.core.dht;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SpringDhtClientFactoryTest {

    @Resource
    private DhtClientFactory dhtClientFactory;

    @Test
    public void shouldReturnNewInstanceOfUpdateAwareDhtWriter() {
        System.err.println("Returning: " + dhtClientFactory.createEmptyBlockingWriter());
        assertTrue(dhtClientFactory.createEmptyBlockingWriter() instanceof UpdateAwareDhtWriter);
    }

    @Test
    public void shouldReturnADifferentInstanceOfUpdateAwareDhtWriterEveryTime() {
        BlockingDhtWriter writer1 = dhtClientFactory.createEmptyBlockingWriter();
        BlockingDhtWriter writer2 = dhtClientFactory.createEmptyBlockingWriter();
        System.err.println("writer 1: " + writer1 + ", writer2: " + writer2);
        assertNotSame(writer1, writer2);
    }

    @Test
    public void shoulReturnNewInstanceOfSimpleDhtReader() {
        System.err.println("Returning: " + dhtClientFactory.createEmptyBlockingReader());
        assertTrue(dhtClientFactory.createEmptyBlockingReader() instanceof SimpleDhtReader);
    }

    @Test
    public void shouldReturnADifferentInstanceOfSimpleDhtReaderEveryTime() {
        BlockingDhtReader reader1 = dhtClientFactory.createEmptyBlockingReader();
        BlockingDhtReader reader2 = dhtClientFactory.createEmptyBlockingReader();
        System.err.println("reader 1: " + reader1 + ", reader 2: " + reader2);
        assertNotSame(reader1, reader2);
    }

    @Test
    public void shouldContainAnExecutorResource() {
        Object executor = ReflectionTestUtils.getField(dhtClientFactory, "executor");
        assertTrue(executor instanceof ThreadPoolTaskExecutor);
    }
}

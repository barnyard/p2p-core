package com.bt.pi.core.util;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class BlockingAspectTest {

    @Resource
    ExampleBlockingOperationForTest exampleBlockingOperation;

    @Resource(name = "dummyBlockingDhtReader")
    BlockingDhtReader dummyBlockingDhtReader;

    @Resource(name = "dummyBlockingDhtWriter")
    BlockingDhtWriter dummyBlockingDhtWriter;

    @Resource(name = "updateAwareDhtWriter")
    private BlockingDhtWriter updateAwareDhtWriter;

    @Resource(name = "simpleDhtReader")
    private BlockingDhtReader simpleDhtReader;

    @Resource
    private DhtClientFactory dhtClientFactory;

    private Exception exceptionReturned;

    @org.junit.Before
    public void setup() {
        this.exceptionReturned = null;
    }

    @Test
    public void shouldThrowExceptionWhenRunningInSelectorThread() {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    BlockingAspect aspect = new BlockingAspect();
                    aspect.throwExceptionIfRunningInSelectorThread(null);
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    assertTrue(t instanceof BlockingOperationCannotRunInSelectorThreadException);
                }

            }

        });

    }

    @Test
    public void shouldThrowExceptionWhenCallingABlockingMethod() throws InterruptedException {

        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    exampleBlockingOperation.blockingMethod();
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }

            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);

    }

    @Test
    public void shouldThrowExceptionWhenInvokingBlockingDhtWriter() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    dummyBlockingDhtWriter.writeIfAbsent(null, null);
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingBlockingDhtReader() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    dummyBlockingDhtReader.get(null);
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingPutInUpdateAwareDhtWriter() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    updateAwareDhtWriter.put(mock(PId.class), mock(PiEntity.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingPutInUpdateAwareDhtWriterFromDhtClientFactory() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
                    writer.put(mock(PId.class), mock(PiEntity.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingUpdateInUpdateAwareDhtWriter() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    updateAwareDhtWriter.update(mock(PId.class), mock(PiEntity.class), mock(UpdateResolver.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingUpdateInUpdateAwareDhtWriterFromDhtClientFactory() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
                    System.err.println(writer);
                    writer.update(mock(PId.class), mock(PiEntity.class), mock(UpdateResolver.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingGetInSimpleDhtReader() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    simpleDhtReader.get(mock(PId.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingGetInSimpleDhtReaderFromDhtClientFactory() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    BlockingDhtReader reader = dhtClientFactory.createBlockingReader();
                    System.err.println(reader);
                    Annotation[] annotations = reader.getClass().getMethod("get", PId.class).getAnnotations();
                    for (Annotation annotation : annotations) {
                        System.err.println("Annotation found " + annotation);
                    }
                    reader.get(mock(PId.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);
                    if (!(t instanceof BlockingOperationCannotRunInSelectorThreadException)) {
                        t.printStackTrace();
                    }

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingGetAnyInSimpleDhtReader() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    simpleDhtReader.getAny(mock(PId.class));

                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingGetAnyInSimpleDhtReaderFromDhtClientFactory() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    BlockingDhtReader reader = dhtClientFactory.createBlockingReader();
                    Annotation[] annotations = reader.getClass().getMethod("getAny", PId.class).getAnnotations();
                    for (Annotation annotation : annotations) {
                        System.err.println("Annotation found " + annotation);
                    }
                    reader.getAny(mock(PId.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);
                    if (!(t instanceof BlockingOperationCannotRunInSelectorThreadException)) {
                        t.printStackTrace();
                    }

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingWriteIfAbsentInUpdateAwareDhtWriter() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    updateAwareDhtWriter.writeIfAbsent(mock(PId.class), mock(PiEntity.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    @Test
    public void shouldThrowExceptionWhenInvokingWriteIfAbsentInUpdateAwareDhtWriterFromDhtClientFactory() throws InterruptedException {
        runBlockingOperationInSelectorThread(new Runnable() {

            @Override
            public void run() {
                try {
                    BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
                    writer.writeIfAbsent(mock(PId.class), mock(PiEntity.class));
                    throw new RuntimeException();
                } catch (Exception t) {
                    System.err.println("Caught Exception: " + t);

                    exceptionReturned = t;
                }
            }

        });
        Thread.sleep(500);
        assertTrue(exceptionReturned instanceof BlockingOperationCannotRunInSelectorThreadException);
    }

    private void runBlockingOperationInSelectorThread(Runnable runnable) {

        Thread mockSelectorThread = new Thread(runnable

        , "Selector Thread");

        mockSelectorThread.start();

    }
}


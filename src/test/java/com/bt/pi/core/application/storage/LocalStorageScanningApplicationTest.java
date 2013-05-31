package com.bt.pi.core.application.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.dht.storage.PersistentDhtStorage;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageScanningApplicationTest {
    @InjectMocks
    private LocalStorageScanningApplication localStorageScanningApplication = new LocalStorageScanningApplication();
    @Mock
    private AlwaysOnApplicationActivator applicationActivator;
    @Mock
    private PersistentDhtStorage persistentDhtStorage;
    private Id id1 = rice.pastry.Id.build("1234");
    private Id id2 = rice.pastry.Id.build("5678");
    @Mock
    private KoalaGCPastMetadata meta1;
    @Mock
    private KoalaGCPastMetadata meta2;
    private SortedMap<Id, KoalaGCPastMetadata> metadataMap;
    private AtomicInteger count1 = new AtomicInteger();
    private AtomicInteger count2 = new AtomicInteger();

    @Before
    public void before() {
        metadataMap = new TreeMap<Id, KoalaGCPastMetadata>() {
            {
                put(id1, meta1);
                put(id2, meta2);
            }
        };
        when(persistentDhtStorage.scanMetadata()).thenReturn(metadataMap);
        this.localStorageScanningApplication.setSleepMillis(1);
    }

    @Test
    public void shouldReturnAlwaysOnApplicationActivator() {
        assertEquals(applicationActivator, this.localStorageScanningApplication.getApplicationActivator());
    }

    @Test
    public void testGetStartTimeout() {
        assertEquals(60, this.localStorageScanningApplication.getStartTimeout());
        assertEquals(TimeUnit.SECONDS, this.localStorageScanningApplication.getStartTimeoutUnit());
    }

    @Test
    public void testBecomeActive() {
        assertTrue(this.localStorageScanningApplication.becomeActive());
    }

    @Test
    public void testGetApplicationName() {
        assertEquals(LocalStorageScanningApplication.class.getSimpleName(), this.localStorageScanningApplication.getApplicationName());
    }

    @Test
    public void testGetActivationCheckPeriodSecs() {
        assertEquals(30, this.localStorageScanningApplication.getActivationCheckPeriodSecs());
    }

    @Test
    public void shouldCallAllHandlersForEachIdInStorage() {
        // setup
        this.localStorageScanningApplication.becomeActive();
        this.localStorageScanningApplication.setHandlers(Arrays.asList(new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                count1.getAndIncrement();
            }
        }, new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                count2.getAndIncrement();
            }
        }));

        // act
        this.localStorageScanningApplication.scan();

        // assert
        assertEquals(2, count1.get());
        assertEquals(2, count2.get());
    }

    @Test
    public void shouldContinueWhenHandlerThrows() {
        // setup
        this.localStorageScanningApplication.becomeActive();
        this.localStorageScanningApplication.setHandlers(Arrays.asList(new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                if (count1.get() == 1)
                    throw new RuntimeException("shit happens");
                count1.getAndIncrement();
            }
        }, new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                count2.getAndIncrement();
            }
        }));

        // act
        this.localStorageScanningApplication.scan();

        // assert
        assertEquals(1, count1.get());
        assertEquals(2, count2.get());
    }

    @Test
    public void shouldNotCallAllHandlersWhenPassive() {
        // setup
        this.localStorageScanningApplication.setHandlers(Arrays.asList(new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                count1.getAndIncrement();
            }
        }, new LocalStorageScanningHandler() {
            @Override
            public void handle(Id id, KoalaGCPastMetadata metadata) {
                count2.getAndIncrement();
            }
        }));

        // act
        this.localStorageScanningApplication.scan();

        // assert
        assertEquals(0, count1.get());
        assertEquals(0, count2.get());
    }
}

package com.bt.pi.core.application.storage;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;

@RunWith(MockitoJUnitRunner.class)
public class LocalStorageScanningHandlerBaseTest {
    private AtomicBoolean called = new AtomicBoolean(false);
    @InjectMocks
    private LocalStorageScanningHandlerBase localStorageScanningHandlerBase = new LocalStorageScanningHandlerBase() {
        @Override
        protected void doHandle(Id id, KoalaGCPastMetadata metadata) {
            System.err.println("HERE");
            called.getAndSet(true);
        }

        @Override
        protected String getEntityType() {
            return "test";
        }
    };
    @Mock
    private KoalaGCPastMetadata metadata;
    @Mock
    private Id id;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private ReportingApplication reportingApplication;

    @Test
    public void testHandle() throws Exception {
        // setup
        when(id.toStringFull()).thenReturn("02002032034242");
        when(metadata.getEntityType()).thenReturn("test");
        when(reportingApplication.getNodeIdFull()).thenReturn("nodeid");
        when(reportingApplication.getLeafNodeHandles()).thenReturn(Collections.EMPTY_SET);
        when(koalaIdUtils.isIdClosestToMe(anyString(), (Collection) any(), (Id) any())).thenReturn(true);

        // act
        localStorageScanningHandlerBase.handle(id, metadata);

        // assert
        Thread.sleep(100);
        assertTrue(called.get());
    }
}

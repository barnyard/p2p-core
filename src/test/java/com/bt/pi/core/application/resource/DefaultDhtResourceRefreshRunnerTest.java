package com.bt.pi.core.application.resource;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.application.resource.leased.LeasedResourceAllocationRecordHeartbeater;
import com.bt.pi.core.continuation.LoggingContinuation;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.ThrowingContinuationAnswer;

public class DefaultDhtResourceRefreshRunnerTest {

    private ConsumedDhtResourceRegistry dhtResourceRegistry;
    private PId resourceId;
    @SuppressWarnings("unchecked")
    private DefaultDhtResourceRefreshRunner runner;
    private LeasedResourceAllocationRecordHeartbeater leasedResourceAllocationRecordHeartbeater;
    private Continuation<PiEntity, Exception> refreshHandlingContinuation;
    private PiEntity piEntity;
    private ThrowingContinuationAnswer throwingAnswer;
    private GenericContinuationAnswer<PiEntity> refreshAnswer;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        piEntity = mock(PiEntity.class);
        resourceId = new PiId("1234567890123456789012345678901234567890", 0);
        dhtResourceRegistry = mock(ConsumedDhtResourceRegistry.class);
        leasedResourceAllocationRecordHeartbeater = mock(LeasedResourceAllocationRecordHeartbeater.class);
        refreshHandlingContinuation = mock(Continuation.class);

        throwingAnswer = new ThrowingContinuationAnswer(new RuntimeException("moo"));
        refreshAnswer = new GenericContinuationAnswer<PiEntity>(piEntity);

        doAnswer(refreshAnswer).when(dhtResourceRegistry).refresh(eq(resourceId), isA(Continuation.class));

        runner = new DefaultDhtResourceRefreshRunner(resourceId, dhtResourceRegistry, leasedResourceAllocationRecordHeartbeater);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldInvokeSharedResourceManagerRefresh() {

        // act
        runner.run();

        // assert
        verify(dhtResourceRegistry).refresh(eq(resourceId), isA(LoggingContinuation.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallBackRefreshHandlingContinuationIfSpecified() {
        // setup
        runner = new DefaultDhtResourceRefreshRunner(resourceId, dhtResourceRegistry, leasedResourceAllocationRecordHeartbeater, refreshHandlingContinuation);

        // act
        runner.run();

        // assert
        verify(refreshHandlingContinuation).receiveResult(isA(PiEntity.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCallBackErrorHandlingContinuationIfErrorThrownOnRefresh() {
        // setup
        runner = new DefaultDhtResourceRefreshRunner(resourceId, dhtResourceRegistry, leasedResourceAllocationRecordHeartbeater, refreshHandlingContinuation);

        doAnswer(throwingAnswer).when(dhtResourceRegistry).refresh(eq(resourceId), isA(Continuation.class));

        // act
        runner.run();

        // assert
        verify(refreshHandlingContinuation).receiveException(isA(RuntimeException.class));
    }

    @Test
    public void shouldTimestampAnyLeasedResourceAllocationsInEntityBeingRefreshed() {
        // act
        runner.run();

        // assert
        verify(leasedResourceAllocationRecordHeartbeater).timestampLeasedAllocatedResources(eq(piEntity));
    }
}

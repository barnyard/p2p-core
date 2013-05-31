package com.bt.pi.core.application.resource.leased;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.continuation.UpdateResolvingContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;

@SuppressWarnings("unchecked")
public class LeasedResourceAllocationRecordHeartbeaterTest {
    private LeasedResourceAllocationRecordHeartbeater leasedResourceAllocationRecordHeartbeater;
    private DhtClientFactory dhtClientFactory;
    private DhtWriter dhtWriter;
    private PId id;
    private LeasedResourceAllocationRecord allocationRecord;
    private UpdateResolvingContinuationAnswer updateResolvingContinuationAnswer;
    private List<Long> resources;
    private List<String> consumerIds;
    private KoalaIdFactory koalaIdFactory;

    public class AnnotatedEntity {
        private Long myLongField;
        private String myStringField;

        public AnnotatedEntity(Long myLongField, String myStringField) {
            this.myLongField = myLongField;
            this.myStringField = myStringField;
        }

        @LeasedAllocatedResource(allocationRecordScope = NodeScope.GLOBAL, allocationRecordUri = "uri:abc")
        public Long getMyLongField() {
            return myLongField;
        }

        @LeasedAllocatedResourceConsumerId
        public String getMyStringField() {
            return myStringField;
        }
    }

    public class AnnotatedEntityWithoutConsumerId {
        private Long myLongField;
        private String myStringField;

        public AnnotatedEntityWithoutConsumerId(Long myLongField, String myStringField) {
            this.myLongField = myLongField;
            this.myStringField = myStringField;
        }

        @LeasedAllocatedResource(allocationRecordScope = NodeScope.GLOBAL, allocationRecordUri = "uri:abc")
        public Long getMyLongField() {
            return myLongField;
        }

        public String getMyStringField() {
            return myStringField;
        }
    }

    @Before
    public void before() {
        id = mock(PId.class);
        when(id.forLocalScope(eq(NodeScope.GLOBAL))).thenReturn(id);

        resources = new ArrayList<Long>();
        resources.add(123L);
        resources.add(456L);

        consumerIds = new ArrayList<String>();
        consumerIds.add("abc");
        consumerIds.add("def");

        allocationRecord = mock(LeasedResourceAllocationRecord.class);
        koalaIdFactory = mock(KoalaIdFactory.class);

        updateResolvingContinuationAnswer = new UpdateResolvingContinuationAnswer(allocationRecord);

        dhtWriter = mock(DhtWriter.class);
        doAnswer(updateResolvingContinuationAnswer).when(dhtWriter).update(eq(id), isA(UpdateResolvingContinuation.class));

        dhtClientFactory = mock(DhtClientFactory.class);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        leasedResourceAllocationRecordHeartbeater = new LeasedResourceAllocationRecordHeartbeater();
        leasedResourceAllocationRecordHeartbeater.setDhtClientFactory(dhtClientFactory);
        leasedResourceAllocationRecordHeartbeater.setKoalaIdFactory(koalaIdFactory);
    }

    @Test
    public void shouldNotHeartbeatIfEntityIsNull() {
        // act
        leasedResourceAllocationRecordHeartbeater.timestampLeasedAllocatedResources(null);

        // assert
        verify(allocationRecord, never()).heartbeat(isA(Object.class), isA(String.class));
    }

    @Test
    public void shouldHeartbeatIfStillAllocated() {
        // act
        leasedResourceAllocationRecordHeartbeater.heartbeat(id, resources, consumerIds);

        // assert
        verify(allocationRecord).heartbeat(123L, "abc");
        verify(allocationRecord).heartbeat(456L, "def");
    }

    @Test
    public void shouldNotWriteIfNotAllocated() {
        // setup
        resources.remove(new Long(123));
        consumerIds.remove("def");

        // act
        leasedResourceAllocationRecordHeartbeater.heartbeat(id, resources, consumerIds);

        // assert
        assertNull(updateResolvingContinuationAnswer.getResult());
    }

    @Test
    public void shouldHeartbeatAnyAnnotatedResources() {
        // setup
        AnnotatedEntity entity = new AnnotatedEntity(1L, "aa");
        when(koalaIdFactory.buildPId(anyString())).thenAnswer(new Answer<PId>() {
            public PId answer(InvocationOnMock invocation) throws Throwable {
                assertEquals("uri:abc", (String) invocation.getArguments()[0]);
                return id;
            }
        });

        // act
        leasedResourceAllocationRecordHeartbeater.timestampLeasedAllocatedResources(entity);

        // assert
        verify(allocationRecord).heartbeat(1L, "aa");
    }

    @Test
    public void shouldCatchAndLogAnyExceptionsWhenHeartbeatingAResource() {
        // setup
        AnnotatedEntity entity = new AnnotatedEntity(1L, "aa");
        when(koalaIdFactory.buildPId(anyString())).thenReturn(id);

        leasedResourceAllocationRecordHeartbeater = mock(LeasedResourceAllocationRecordHeartbeater.class);
        doThrow(new RuntimeException("oops")).when(leasedResourceAllocationRecordHeartbeater).heartbeat(any(PId.class), any(List.class), any(List.class));

        leasedResourceAllocationRecordHeartbeater.setKoalaIdFactory(koalaIdFactory);

        // act
        leasedResourceAllocationRecordHeartbeater.timestampLeasedAllocatedResources(entity);

        // assert
        verify(allocationRecord, never()).heartbeat(1L, "aa");
    }

    @Test(expected = IncorrectNumberOfAnnotationsException.class)
    public void shouldThrowIfAnnotatedResourceHasNoCorrespondingConsumerIdAnnotation() throws Exception {
        // setup
        AnnotatedEntityWithoutConsumerId entity = new AnnotatedEntityWithoutConsumerId(1L, "aa");

        Method method = AnnotatedEntityWithoutConsumerId.class.getMethod("getMyLongField");

        // act
        leasedResourceAllocationRecordHeartbeater.timestampLeasedAllocatedResource(id, entity, method, AnnotatedEntityWithoutConsumerId.class);
    }
}

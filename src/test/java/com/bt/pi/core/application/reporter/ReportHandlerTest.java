/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.reporter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.node.NodeStartedEvent;

@RunWith(MockitoJUnitRunner.class)
public class ReportHandlerTest {
    @Mock
    private ReportingApplication reportingApplication;
    @Mock
    private ReportableEntityStore<MyPiEntity> reportableEntityStore;
    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @InjectMocks
    MyReportHandler reportHandler = new MyReportHandler();

    @Test
    public void shouldPersistWhenReceivingData() throws Exception {
        // setup
        MyPiEntity reportableEntity = mock(MyPiEntity.class);

        // act
        reportHandler.receive(reportableEntity);

        // assert
        verify(reportableEntityStore).add(reportableEntity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPersistWhenReceivingPiEntityCollection() throws Exception {
        // setup
        Collection<MyPiEntity> entities = mock(Collection.class);
        MyPiEntityCollection piEntityCollection = mock(MyPiEntityCollection.class);
        when(piEntityCollection.getEntities()).thenReturn(entities);

        // act
        reportHandler.receiveFromNode(piEntityCollection);

        // assert
        verify(reportableEntityStore).addAll(entities);
    }

    @Test
    public void shouldNotPublishUpdatesIfNodeNotYetStarted() throws Exception {
        // act
        reportHandler.publishUpdates();

        // assert
        verify(reportingApplication, never()).publishToReportingTopic(isA(PiEntity.class));
    }

    @Test
    public void shouldNotPublishUpdatesIfNodeStartedNothingToPublish() throws Exception {
        // setup
        reportHandler.onApplicationEvent(new NodeStartedEvent(this));

        // act
        reportHandler.publishUpdates();

        // assert
        verify(reportingApplication, never()).publishToReportingTopic(isA(PiEntity.class));
    }

    @Test
    public void shouldPublishUpdates() throws Exception {
        // setup
        final MyPiEntity piEntity1 = new MyPiEntity(null);
        final MyPiEntity piEntity2 = new MyPiEntity(null);
        Collection<MyPiEntity> piEntities = new ArrayList<MyPiEntity>();
        piEntities.add(piEntity1);
        piEntities.add(piEntity2);
        when(reportableEntityStore.getPublishIterator()).thenReturn(piEntities.iterator());
        reportHandler.onApplicationEvent(new NodeStartedEvent(this));

        // act
        reportHandler.publishUpdates();

        // assert
        verify(reportingApplication).publishToReportingTopic(argThat(new ArgumentMatcher<MyPiEntityCollection>() {
            @Override
            public boolean matches(Object argument) {
                MyPiEntityCollection piEntityCollection = (MyPiEntityCollection) argument;
                assertThat(piEntityCollection.getEntities().size(), equalTo(2));
                return piEntityCollection.getEntities().contains(piEntity1) && piEntityCollection.getEntities().contains(piEntity2);
            }
        }));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfReportableEntityStoreNotSet() throws Exception {
        // setup
        reportHandler.setReportableEntityStore(null);

        // act
        reportHandler.checkReportableEntityStoreHasBeenSetAndStartScheduledThread();
    }

    @Test
    public void shouldSpinOffSchedulerThreadOnPostConstruct() throws Exception {
        // act
        reportHandler.checkReportableEntityStoreHasBeenSetAndStartScheduledThread();

        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0l), eq(300l), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldRemoveEntitiesWithSameTimeAndIdIfReceiveFromSuperNode() {
        // setup
        PiEntityCollection<MyPiEntity> entityCollection = reportHandler.getPiEntityCollection();
        MyPiEntity entity1 = new MyPiEntity("node 1");
        MyPiEntity entity2 = new MyPiEntity("node 2");
        entityCollection.setEntities(new ArrayList<ReportHandlerTest.MyPiEntity>());
        entityCollection.getEntities().add(entity1);
        entityCollection.getEntities().add(entity2);
        when(reportableEntityStore.exists(entity1)).thenReturn(true);
        when(reportableEntityStore.exists(entity2)).thenReturn(false);

        // act
        reportHandler.receive(entityCollection, true);

        // assert
        assertThat(entityCollection.getEntities().size(), equalTo(1));
        System.err.println(entityCollection.getEntities());
        assertTrue(entityCollection.getEntities().contains(entity2));
        verify(reportableEntityStore).addAll(entityCollection.getEntities());
    }

    @Test
    public void shouldSetBroadcastWindowSize() {
        // act
        reportHandler.setBroadcastWindowSize(100);
        int broadcastWindowSize = (Integer) ReflectionTestUtils.getField(reportHandler, "broadcastWindowSize");
        // assert
        assertEquals(100, broadcastWindowSize);
    }

    @Test
    public void shouldSetReportingApplication() {
        // setup
        ReportingApplication reportingApplication = mock(ReportingApplication.class);

        // act
        reportHandler.setReportingApplication(reportingApplication);
        ReportingApplication reportingAppInObject = (ReportingApplication) ReflectionTestUtils.getField(reportHandler, "reportingApplication");
        // assert
        assertEquals(reportingApplication, reportingAppInObject);
    }

    @Test
    public void shouldSetScheduledExecutorService() {
        // setup
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        // act
        reportHandler.setScheduledExecutorService(scheduledExecutorService);
        ScheduledExecutorService serviceInObject = (ScheduledExecutorService) ReflectionTestUtils.getField(reportHandler, "scheduledExecutorService");
        // assert
        assertEquals(scheduledExecutorService, serviceInObject);
    }

    private class MyPiEntity extends ReportableEntity<MyPiEntity> {
        public MyPiEntity(String nodeId) {
            super(nodeId);
        }

        @Override
        public String toString() {
            return "MyPiEntity: " + getNodeId() + ", " + getCreationTime();
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public Object[] getKeysForMap() {
            return null;
        }

        @Override
        public int getKeysForMapCount() {
            return 0;
        }

        @Override
        public int compareTo(MyPiEntity o) {
            return 0;
        }

        @Override
        public String getType() {
            return this.getClass().getName();
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public long getCreationTime() {
            return new Date().getTime();
        }

        @Override
        public String getUriScheme() {
            return this.getClass().getName();
        }
    }

    private class MyPiEntityCollection extends PiEntityCollection<MyPiEntity> {
        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getUriScheme() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private class MyReportHandler extends ReportHandler<MyPiEntity> {
        public MyReportHandler() {
            super(null);
        }

        @Override
        protected PiEntityCollection<MyPiEntity> getPiEntityCollection() {
            return new MyPiEntityCollection();
        }
    }
}

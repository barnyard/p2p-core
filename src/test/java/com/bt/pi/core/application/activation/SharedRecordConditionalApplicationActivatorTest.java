package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.application.EchoApplication;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.SubDhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.PiInsufficientResultsException;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.scope.NodeScope;

@SuppressWarnings("unchecked")
public class SharedRecordConditionalApplicationActivatorTest {
    private static final long START_WAIT_TIMEOUT = 2000L;
    private static final String APP_NAME = "APP_NAME";
    private SharedRecordConditionalApplicationActivator activator;
    private KoalaDHTStorage storage;
    private DhtClientFactory dhtClientFactory;
    private ApplicationRegistry appRegistry;
    private List<String> mutuallyExclusiveApps;
    private ApplicationRecord appRecord;
    private Boolean[] insertResults;
    private EchoApplication echoApp;
    private Collection<NodeHandle> leafsetHandles;
    private ThreadPoolTaskExecutor executor;
    private ScheduledExecutorService scheduledExecutor;
    private KoalaIdFactory koalaIdFactory;
    private Id nodeId;
    private Id modeId;
    private Id codeId;
    private Semaphore runnableExecutedSemaphore;
    private Boolean applicationActivationCommenced;
    private CountDownLatch applicationActivationRolledBackLatch;
    private CountDownLatch passiveStateSetLatch;
    private Semaphore insertedSemaphore;
    private ApplicationContext applicationContext;
    private InterApplicationDependenciesStore interApplicationDependenciesStore;
    private int activationCheckSeconds;
    private String echoAppName = "echo-app";

    @SuppressWarnings({ "serial", "rawtypes" })
    @Before
    public void before() {
        interApplicationDependenciesStore = mock(InterApplicationDependenciesStore.class);
        passiveStateSetLatch = new CountDownLatch(1);
        insertedSemaphore = new Semaphore(0);

        applicationContext = mock(ApplicationContext.class);

        appRegistry = new ApplicationRegistry() {
            @Override
            public synchronized void setApplicationStatus(String applicationName, ApplicationStatus status) {
                super.setApplicationStatus(applicationName, status);
                if (status == ApplicationStatus.PASSIVE)
                    passiveStateSetLatch.countDown();
            }
        };
        koalaIdFactory = new KoalaIdFactory();
        koalaIdFactory.setRegion(0);
        koalaIdFactory.setAvailabilityZone(0);
        koalaIdFactory.setKoalaPiEntityFactory(new KoalaPiEntityFactory());

        storage = mock(KoalaDHTStorage.class);

        nodeId = koalaIdFactory.buildId("nodeId");
        modeId = koalaIdFactory.buildId("modeId");
        codeId = koalaIdFactory.buildId("codeId");

        runnableExecutedSemaphore = new Semaphore(0);
        applicationActivationCommenced = false;
        applicationActivationRolledBackLatch = new CountDownLatch(1);
        scheduledExecutor = Executors.newScheduledThreadPool(4);

        executor = new ThreadPoolTaskExecutor() {
            @Override
            public void execute(final Runnable runnable) {
                super.execute(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                        runnableExecutedSemaphore.release();
                    }
                });
            }
        };
        executor.initialize();

        dhtClientFactory = new SubDhtClientFactory();
        dhtClientFactory.setKoalaDhtStorage(storage);
        dhtClientFactory.setExecutor(executor);

        mutuallyExclusiveApps = new ArrayList<String>();
        mutuallyExclusiveApps.add(APP_NAME);
        NodeHandle mockNodeHandle = mock(NodeHandle.class);
        when(mockNodeHandle.getNodeId()).thenReturn((rice.pastry.Id) nodeId);
        leafsetHandles = Arrays.asList(new NodeHandle[] { mockNodeHandle });
        echoApp = mock(EchoApplication.class);
        when(echoApp.getNodeId()).thenReturn(nodeId);
        when(echoApp.getApplicationName()).thenReturn(echoAppName);
        when(echoApp.getPreferablyExcludedApplications()).thenReturn(mutuallyExclusiveApps);
        when(echoApp.getLeafNodeHandles()).thenReturn(leafsetHandles);
        activationCheckSeconds = 60;
        when(echoApp.getActivationCheckPeriodSecs()).thenReturn(activationCheckSeconds);
        when(echoApp.getStartTimeout()).thenReturn(START_WAIT_TIMEOUT);
        when(echoApp.getStartTimeoutUnit()).thenReturn(TimeUnit.MILLISECONDS);

        appRecord = new RegionScopedApplicationRecord(APP_NAME);

        activator = new SharedRecordConditionalApplicationActivator() {
            @Override
            protected void executeApplicationActivation(ActivationAwareApplication app) {
                applicationActivationCommenced = true;
                super.executeApplicationActivation(app);
            }

            @Override
            protected void rollbackApplicationActivation(ActivationAwareApplication application) {
                super.rollbackApplicationActivation(application);
                applicationActivationRolledBackLatch.countDown();
            }

            @Override
            public NodeScope getActivationScope() {
                return NodeScope.REGION;
            }
        };
        activator.setApplicationRegistry(appRegistry);
        activator.setDhtClientFactory(dhtClientFactory);
        activator.setExecutor(executor);
        activator.setScheduledExecutorService(scheduledExecutor);
        activator.setKoalaIdFactory(koalaIdFactory);
        activator.setApplicationContext(applicationContext);
        ReflectionTestUtils.setField(activator, "interApplicationDependenciesStore", interApplicationDependenciesStore);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult(appRecord);
                return null;
            }
        }).when(storage).get(isA(PId.class), isA(Continuation.class));

        insertResults = new Boolean[] { true, true, true };

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[2];
                if (!insertResults[0]) {
                    c.receiveException(new PiInsufficientResultsException("boooo"));
                } else
                    c.receiveResult(insertResults);
                insertedSemaphore.release();
                return null;
            }
        }).when(storage).put(isA(PId.class), isA(ApplicationRecord.class), isA(Continuation.class));
    }

    @Test
    public void shouldGetNearestNodeToGivenIdFromAppRecordWhenOneCached() {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 3);
        appRecord.addCurrentlyActiveNode(nodeId, -1);
        appRecord.addCurrentlyActiveNode(modeId, -1);
        appRecord.addCurrentlyActiveNode(codeId, -1);

        appRegistry = mock(ApplicationRegistry.class);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        activator.setApplicationRegistry(appRegistry);

        // act
        Id res = activator.getClosestActiveApplicationNodeId(APP_NAME, codeId);

        // assert
        assertEquals(codeId, res);
    }

    @Test
    public void shouldGetNullNearestNodeFromAppRecordWhenAppRecordEmpty() {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        activator.setApplicationRegistry(appRegistry);

        // act
        Id res = activator.getClosestActiveApplicationNodeId(APP_NAME, codeId);

        // assert
        assertEquals(null, res);
    }

    @Test
    public void shouldGetNullNearestNodeFromAppRecordWhenAppRecordNotCached() {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(null);
        activator.setApplicationRegistry(appRegistry);

        // act
        Id res = activator.getClosestActiveApplicationNodeId(APP_NAME, codeId);

        // assert
        assertEquals(null, res);
    }

    @Test
    public void wontStartWhenAnotherAppIsActive() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.PASSIFY, res);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldCacheApplicationRecordEvenIfGoingPassive() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        when(interApplicationDependenciesStore.getPreferablyExcludedApplications(echoApp.getApplicationName())).thenReturn(mutuallyExclusiveApps);

        rice.pastry.Id mockNodeId = mock(rice.pastry.Id.class);
        when(mockNodeId.toStringFull()).thenReturn("000034567890123456789012345678");
        NodeHandle leafsetNode = mock(NodeHandle.class);
        when(leafsetNode.getNodeId()).thenReturn(mockNodeId);
        when(echoApp.getLeafNodeHandles()).thenReturn(Arrays.asList(leafsetNode));

        final ApplicationRecord echoAppRecord = mock(RegionScopedApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Continuation c = (Continuation) invocation.getArguments()[1];
                c.receiveResult(echoAppRecord);
                return null;
            }
        }).when(storage).get(isA(PId.class), isA(Continuation.class));

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.PASSIFY, res);
        verify(appRegistry).setCachedApplicationRecord(echoAppName, echoAppRecord);
    }

    @Test
    public void wontStartWhenAnotherAppIsChecking() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.CHECKING);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.RETRY, res);
    }

    @Test
    public void willStartIfMorePreferablyExcludedAppsThanLeafset() {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.CHECKING);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);

        when(interApplicationDependenciesStore.getPreferablyExcludedApplications(echoApp.getApplicationName())).thenReturn(Arrays.asList(APP_NAME));

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.ACTIVATE, res);
    }

    @Test
    public void willAttemptToStartWhenExtraApplicationsRequired() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        verify(echoApp, times(1)).becomeActive();
        assertEquals(appRecord, appRegistry.getCachedApplicationRecord(echoApp.getApplicationName()));
    }

    @Test
    public void willUpdateAppManagerToActiveWhenStartCompletesSuccessfully() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);

        when(echoApp.becomeActive()).thenReturn(true);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        assertEquals(ApplicationStatus.ACTIVE, appRegistry.getApplicationStatus(echoApp.getApplicationName()));
        verify(echoApp, times(1)).becomeActive();
        assertTrue(appRecord.containsNodeId(nodeId));
    }

    @Test
    public void willRaiseAppRecordUpdatedEventWhenApplicationBecomesActive() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);

        when(echoApp.becomeActive()).thenReturn(true);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(insertedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        verify(applicationContext).publishEvent(argThat(new ArgumentMatcher<ApplicationRecordRefreshedEvent>() {
            @Override
            public boolean matches(Object argument) {
                ApplicationRecordRefreshedEvent arg = (ApplicationRecordRefreshedEvent) argument;
                assertEquals(appRecord, arg.getApplicationRecord());
                return true;
            }
        }));
    }

    /**
     * If start returns false mode should be passive
     */
    @Test
    public void willUpdateAppManagerToPassiveWhenStartReturnFalse() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);

        when(echoApp.becomeActive()).thenReturn(false);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        assertEquals(ApplicationStatus.PASSIVE, appRegistry.getApplicationStatus(echoApp.getApplicationName()));
        verify(echoApp).becomePassive();
        assertFalse(appRecord.getActiveNodeMap().containsValue(nodeId.toStringFull()));
    }

    @Test
    public void willRaiseAppRecordUpdatedEventWhenApplicationActivationFails() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);

        when(echoApp.becomeActive()).thenReturn(false);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(insertedSemaphore.tryAcquire(2, 5, TimeUnit.SECONDS));
        verify(applicationContext, times(2)).publishEvent(argThat(new ArgumentMatcher<ApplicationRecordRefreshedEvent>() {
            @Override
            public boolean matches(Object argument) {
                ApplicationRecordRefreshedEvent arg = (ApplicationRecordRefreshedEvent) argument;
                assertEquals(appRecord, arg.getApplicationRecord());
                return true;
            }
        }));
    }

    /**
     * tests that the case of a start taking too long. App should go back to passive when it takes too long.
     */
    @Test
    public void willUpdateAppManagerToPassiveWhenCheckTakesTooLong() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);
        when(echoApp.becomeActive()).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(START_WAIT_TIMEOUT * 2);
                return true;
            }
        });

        // act
        activator.register(echoApp);

        // verify
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        assertEquals(ApplicationStatus.PASSIVE, appRegistry.getApplicationStatus(echoApp.getApplicationName()));
        verify(echoApp, times(1)).becomePassive();
        assertFalse(appRecord.getActiveNodeMap().containsValue(nodeId.toStringFull()));
    }

    /**
     * Tests that the application will be standyby and the executor won't be invoked when no more live nodes are needed.
     */
    @Test
    public void wontStartWhenExtraApplicationsAreNotNeededRequired() throws Exception {
        // setup
        appRecord = new GlobalScopedApplicationRecord(APP_NAME, 12, 2);
        appRecord.addCurrentlyActiveNode(koalaIdFactory.buildId("one"), -1);
        appRecord.addCurrentlyActiveNode(koalaIdFactory.buildId("two"), -1);

        // act
        activator.register(echoApp);

        // verify
        assertTrue(passiveStateSetLatch.await(5, TimeUnit.SECONDS));
        assertFalse(applicationActivationCommenced);
        assertEquals(ApplicationStatus.PASSIVE, appRegistry.getApplicationStatus(echoApp.getApplicationName()));
    }

    /**
     * Tests that the application is set to standby if the insert into the dht fails
     */
    @Test
    public void wontStartWhenTheInsertsFails() throws Exception {
        // setup
        insertResults[0] = false;

        // act
        activator.register(echoApp);

        // verify
        assertTrue(passiveStateSetLatch.await(5, TimeUnit.SECONDS));
        assertFalse(applicationActivationCommenced);
        assertEquals(ApplicationStatus.PASSIVE, appRegistry.getApplicationStatus(echoApp.getApplicationName()));
    }

    @Test
    public void shouldRemoveNodeIdFromApplicationRecord() throws Exception {
        // setup
        Id otherNodeId = koalaIdFactory.buildId("one");
        appRecord.addCurrentlyActiveNode(otherNodeId, -1);
        appRegistry = mock(ApplicationRegistry.class);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.CHECKING);
        when(echoApp.getPreferablyExcludedApplications()).thenReturn(new ArrayList<String>());
        activator.setApplicationRegistry(appRegistry);
        when(echoApp.becomeActive()).thenReturn(true);

        // act
        activator.deActivateNode(otherNodeId.toStringFull(), echoApp);

        // verify
        assertFalse(appRecord.containsNodeId(otherNodeId));
        assertTrue(appRecord.containsNodeId(nodeId));
    }

    @Test
    public void shouldHeartbeatActiveAppInAppRecord() throws Exception {
        // setup
        appRecord = spy(new GlobalScopedApplicationRecord(APP_NAME, 12, 2));
        when(echoApp.becomeActive()).thenReturn(true);

        activator.register(echoApp);
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        long cachedAppRecordVersionAfterActivation = appRegistry.getCachedApplicationRecord("echo-app").getVersion();

        // act
        activator.checkActiveApplicationStillActiveAndHeartbeat(echoApp);

        // assert
        assertTrue(insertedSemaphore.tryAcquire(2, 5, TimeUnit.SECONDS));
        verify(applicationContext, times(2)).publishEvent(argThat(new ArgumentMatcher<ApplicationRecordRefreshedEvent>() {
            @Override
            public boolean matches(Object argument) {
                ApplicationRecordRefreshedEvent arg = (ApplicationRecordRefreshedEvent) argument;
                assertEquals(appRecord, arg.getApplicationRecord());
                return true;
            }
        }));
        assertTrue(cachedAppRecordVersionAfterActivation < appRegistry.getCachedApplicationRecord("echo-app").getVersion());
    }

    @Test
    public void shouldPassivateActiveAppWhenHeartbeatFails() throws Exception {
        // setup
        appRecord = spy(new GlobalScopedApplicationRecord(APP_NAME, 12, 2));

        when(echoApp.becomeActive()).thenReturn(true);

        activator.register(echoApp);
        assertTrue(runnableExecutedSemaphore.tryAcquire(1, 5, TimeUnit.SECONDS));
        long cachedAppRecordVersionAfterActivation = appRegistry.getCachedApplicationRecord("echo-app").getVersion();

        appRecord.getActiveNodeMap().put(appRecord.getAssociatedResource(nodeId), new TimeStampedPair<String>("123"));

        // act
        activator.checkActiveApplicationStillActiveAndHeartbeat(echoApp);

        // assert
        assertTrue(applicationActivationRolledBackLatch.await(5, TimeUnit.SECONDS));
        verify(echoApp).becomePassive();
        assertTrue(cachedAppRecordVersionAfterActivation > 12);
    }

    @Test
    public void willStartWhenAnotherAppIsActiveButLeafsetSizeIsTooSmall() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        when(interApplicationDependenciesStore.getPreferablyExcludedApplications(echoApp.getApplicationName())).thenReturn(Arrays.asList(new String[] { APP_NAME }));

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.ACTIVATE, res);
    }

    @Test
    public void willNotStartWhenMultipleInstancesOfAppsArePresentButLeafsetSizeIsLargeEnough() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        appRecord = new RegionScopedApplicationRecord(APP_NAME, 1, 2);

        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        rice.pastry.Id mockNodeId2 = mock(rice.pastry.Id.class);
        when(mockNodeId2.toStringFull()).thenReturn("000094567890123456789012345678");
        NodeHandle leafsetNode2 = mock(NodeHandle.class);
        when(leafsetNode2.getNodeId()).thenReturn(mockNodeId2);

        rice.pastry.Id mockNodeId = mock(rice.pastry.Id.class);
        when(mockNodeId.toStringFull()).thenReturn("000034567890123456789012345678");
        NodeHandle leafsetNode = mock(NodeHandle.class);
        when(leafsetNode.getNodeId()).thenReturn(mockNodeId);
        when(echoApp.getLeafNodeHandles()).thenReturn(Arrays.asList(leafsetNode, leafsetNode2));

        when(interApplicationDependenciesStore.getPreferablyExcludedApplications(echoApp.getApplicationName())).thenReturn(Arrays.asList(APP_NAME));

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.PASSIFY, res);
    }

    @Test
    public void willStartWhenMultipleInstancesOfAppsArePresentButLeafsetSizeIsTooSmall() throws Exception {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);

        appRecord = new RegionScopedApplicationRecord(APP_NAME, 1, 2);
        ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);
        when(echoAppRecord.getRequiredActive()).thenReturn(1);

        rice.pastry.Id mockNodeId = mock(rice.pastry.Id.class);
        when(mockNodeId.toStringFull()).thenReturn("001234567890123456789012345678");
        NodeHandle leafsetNode = mock(NodeHandle.class);
        when(leafsetNode.getNodeId()).thenReturn(mockNodeId);
        when(echoApp.getLeafNodeHandles()).thenReturn(Arrays.asList(leafsetNode));

        when(interApplicationDependenciesStore.getPreferablyExcludedApplications(echoApp.getApplicationName())).thenReturn(Arrays.asList(new String[] { APP_NAME }));

        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);
        when(appRegistry.getApplicationStatus(APP_NAME)).thenReturn(ApplicationStatus.ACTIVE);
        when(appRegistry.getCachedApplicationRecord(APP_NAME)).thenReturn(appRecord);
        when(appRegistry.getCachedApplicationRecord(echoApp.getApplicationName())).thenReturn(echoAppRecord);

        // act
        ApplicationActivationCheckStatus res = activator.checkLocalActivationPreconditions(echoApp);

        // verify
        assertEquals(ApplicationActivationCheckStatus.ACTIVATE, res);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void checkAndActivateShouldAddNodeToApplicationRecordWithCorrectExpiryValue() {
        // setup
        appRegistry = mock(ApplicationRegistry.class);
        activator.setApplicationRegistry(appRegistry);
        when(appRegistry.getApplicationStatus(echoApp.getApplicationName())).thenReturn(ApplicationStatus.NOT_INITIALIZED);

        dhtClientFactory = mock(DhtClientFactory.class);
        activator.setDhtClientFactory(dhtClientFactory);
        DhtWriter writer = mock(DhtWriter.class);

        final ApplicationRecord echoAppRecord = mock(ApplicationRecord.class);

        when(dhtClientFactory.createWriter()).thenReturn(writer);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolvingPiContinuation cont = (UpdateResolvingPiContinuation) invocation.getArguments()[2];
                cont.update(echoAppRecord, echoAppRecord);
                return null;
            }
        }).when(writer).update(isA(PId.class), (PiEntity) Matchers.isNull(), isA(UpdateResolvingPiContinuation.class));

        // act
        activator.checkAndActivate(echoApp, null);

        // assert
        long expectedExpiry = (long) ((activationCheckSeconds + Integer.parseInt(ApplicationActivatorBase.DEFAULT_MAX_VALUE_FOR_RANDOM_INTERVAL_OFFSET_SECONDS)) * Double
                .parseDouble(SharedRecordConditionalApplicationActivator.DEFAULT_ACTIVATION_EXPIRY_MULTIPLICATION_FACTOR));
        verify(echoAppRecord).addCurrentlyActiveNode(isA(Id.class), Matchers.eq(expectedExpiry));
    }
}

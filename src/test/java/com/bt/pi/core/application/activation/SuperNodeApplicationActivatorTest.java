package com.bt.pi.core.application.activation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.KoalaIdUtils;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

@RunWith(MockitoJUnitRunner.class)
public class SuperNodeApplicationActivatorTest {
    private static final int REGION = 1;
    private static final int AVAILABILITY_ZONE = 2;
    private static final String SAME_AVAIL_ZONE_ID = "0102";
    private static final String DIFF_AVAIL_ZONE_ID = "0103";
    private static final String DIFF_REGION_ID = "0202";
    private static final String SUPER_NODE_CHECKPOINT = "checkpoint";

    private Collection<NodeHandle> leafNodeHandles;

    @Mock
    private rice.pastry.Id localNodeId;
    @Mock
    private PId superNodeCheckPointsPId;
    @Mock
    private Id superNodeCheckPointsId;
    @Mock
    private SuperNodeApplicationCheckPoints superNodeApplicationCheckPoints;
    @Mock
    private ActivationAwareApplication application;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private KoalaIdUtils koalaIdUtils;
    @Mock
    private Executor executor;
    @Mock
    private ApplicationRegistry applicationRegistry;
    @Mock
    private TimerTask timerTask;

    @InjectMocks
    private SuperNodeApplicationActivator superNodeApplicationActivator = new SuperNodeApplicationActivator();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        setupLeafNodeHandles();

        when(application.getApplicationName()).thenReturn("appname");
        when(application.getLeafNodeHandles()).thenReturn(leafNodeHandles);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));

        when(localNodeId.toStringFull()).thenReturn("0102000");

        when(koalaIdFactory.buildPId(SuperNodeApplicationCheckPoints.URL)).thenReturn(superNodeCheckPointsPId);
        when(koalaIdFactory.buildPId(SUPER_NODE_CHECKPOINT)).thenReturn(superNodeCheckPointsPId);
        when(koalaIdFactory.buildId(SUPER_NODE_CHECKPOINT)).thenReturn(superNodeCheckPointsId);
        when(koalaIdFactory.generateNodeId()).thenReturn(localNodeId);
        when(koalaIdFactory.getRegion()).thenReturn(REGION);
        when(koalaIdFactory.getAvailabilityZoneWithinRegion()).thenReturn(AVAILABILITY_ZONE);

        Set<String> superNodeCheckPoints = new TreeSet<String>(Arrays.asList(new String[] { SUPER_NODE_CHECKPOINT }));
        when(superNodeApplicationCheckPoints.getSuperNodeCheckPoints(application.getApplicationName(), REGION, AVAILABILITY_ZONE)).thenReturn(superNodeCheckPoints);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PiContinuation<SuperNodeApplicationCheckPoints>) invocation.getArguments()[1]).handleResult(superNodeApplicationCheckPoints);
                return null;
            }
        }).when(dhtCache).get(eq(superNodeCheckPointsPId), isA(PiContinuation.class));

        when(koalaIdUtils.getNodeIdClosestToId(isA(SortedSet.class), eq(superNodeCheckPointsId), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(localNodeId);
    }

    private void setupLeafNodeHandles() {
        leafNodeHandles = new ArrayList<NodeHandle>();
        leafNodeHandles.add(getNewNodeHandle(SAME_AVAIL_ZONE_ID + "1"));
        leafNodeHandles.add(getNewNodeHandle(SAME_AVAIL_ZONE_ID + "2"));
        leafNodeHandles.add(getNewNodeHandle(SAME_AVAIL_ZONE_ID + "3"));
        leafNodeHandles.add(getNewNodeHandle(DIFF_AVAIL_ZONE_ID + "4"));
        leafNodeHandles.add(getNewNodeHandle(DIFF_AVAIL_ZONE_ID + "5"));
        leafNodeHandles.add(getNewNodeHandle(DIFF_REGION_ID + "6"));
    }

    private NodeHandle getNewNodeHandle(String id) {
        rice.pastry.Id nodeHandleId = mock(rice.pastry.Id.class);
        when(nodeHandleId.toStringFull()).thenReturn(id);

        NodeHandle nodeHandle = mock(NodeHandle.class);
        when(nodeHandle.getNodeId()).thenReturn(nodeHandleId);

        return nodeHandle;
    }

    @Test
    public void shouldReturnTrueForLocalActivationChecks() throws Exception {
        // act
        ApplicationActivationCheckStatus result = superNodeApplicationActivator.checkLocalActivationPreconditions(application);

        // assert
        assertThat(result, is(ApplicationActivationCheckStatus.ACTIVATE));
    }

    @Test
    public void shouldGetRightSuperNodeCheckPointsIfScopeIsAvailabilityZone() throws Exception {
        // act
        superNodeApplicationActivator.checkAndActivate(application, timerTask);

        // assert
        verify(superNodeApplicationCheckPoints).getSuperNodeCheckPoints(application.getApplicationName(), REGION, AVAILABILITY_ZONE);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldOnlyLookAtLeafsetNodesInSameAvailabilityZone() throws Exception {
        // act
        superNodeApplicationActivator.checkAndActivate(application, timerTask);

        // assert

        verify(koalaIdUtils).getNodeIdClosestToId(argThat(new ArgumentMatcher<SortedSet<String>>() {
            @Override
            public boolean matches(Object argument) {
                SortedSet<String> leafset = (SortedSet<String>) argument;
                assertThat(leafset.toString(), leafset.size(), equalTo(4));
                for (String leaf : leafset) {
                    assertThat(leaf.startsWith(SAME_AVAIL_ZONE_ID), is(true));
                }
                return true;
            }
        }), eq(superNodeCheckPointsId), eq(NodeScope.AVAILABILITY_ZONE));
    }

    @Test
    public void shouldActivateApplicationIfNodeIsClosestToSuperNodeMarker() throws Exception {
        // act
        superNodeApplicationActivator.checkAndActivate(application, timerTask);

        // assert
        verify(application).becomeActive();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassivateApplicationIfNodeIsNotClosestToAnySuperNodeMarker() throws Exception {
        // setup
        Id differentId = mock(Id.class);
        when(koalaIdUtils.getNodeIdClosestToId(isA(SortedSet.class), eq(superNodeCheckPointsId), eq(NodeScope.AVAILABILITY_ZONE))).thenReturn(differentId);

        // act
        superNodeApplicationActivator.checkAndActivate(application, timerTask);

        // assert
        verify(timerTask).cancel();
        verify(applicationRegistry).setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldPassivateApplicationIfSuperNodeCheckPointsIsNullInDht() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((PiContinuation<SuperNodeApplicationCheckPoints>) invocation.getArguments()[1]).handleResult(null);
                return null;
            }
        }).when(dhtCache).get(eq(superNodeCheckPointsPId), isA(PiContinuation.class));

        // act
        superNodeApplicationActivator.checkAndActivate(application, timerTask);

        // assert
        verify(timerTask).cancel();
        verify(applicationRegistry).setApplicationStatus(application.getApplicationName(), ApplicationStatus.PASSIVE);
    }
}

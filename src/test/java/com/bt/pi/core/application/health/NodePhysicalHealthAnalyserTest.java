/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodePhysicalHealthAnalyser.class)
public class NodePhysicalHealthAnalyserTest {
    private NodePhysicalHealthAnalyser analyser;
    private HeartbeatEntity heartbeat;
    private Collection<String> leafset;
    private Map<String, Long> disks;
    private Collection<Long> diskSizes;

    @SuppressWarnings("unchecked")
    @Before
    public void doBefore() {
        analyser = new NodePhysicalHealthAnalyser();
        heartbeat = mock(HeartbeatEntity.class);
        leafset = mock(Collection.class);
        disks = mock(Map.class);
        diskSizes = new ArrayList<Long>();

        mockStatic(System.class);

        when(heartbeat.getLeafSet()).thenReturn(leafset);
        when(heartbeat.getDiskSpace()).thenReturn(disks);
        when(disks.values()).thenReturn(diskSizes);
        when(leafset.isEmpty()).thenReturn(false);
    }

    @Test
    public void itShouldStopPiWhenLeafsetBecomesEmpty() {
        // setup
        when(leafset.isEmpty()).thenReturn(true, false);

        // act
        analyser.acceptHeartbeat(heartbeat, false);
        analyser.acceptHeartbeat(heartbeat, false);

        // assert
        PowerMockito.verifyStatic();
        System.exit(0);
    }

    @Test
    public void itShouldNotStopPiIfLeafsetNeverHadAnyNodes() {
        // setup
        when(leafset.isEmpty()).thenReturn(true, true, true);

        // act
        analyser.acceptHeartbeat(heartbeat, false);
        analyser.acceptHeartbeat(heartbeat, false);

        // assert
        PowerMockito.verifyStatic(never());
        System.exit(0);
    }

    @Test
    public void itShouldStopPiIfAnyDiskSpaceIsLessThan100M() {
        // setup
        diskSizes.add(50l);

        // act
        analyser.acceptHeartbeat(heartbeat, false);

        // assert
        PowerMockito.verifyStatic();
        System.exit(0);
    }

    @Test
    public void itShouldStopPiIfAnyDiskSpaceIsLessThan100MWhenAllOthersAreMore() {
        // setup
        diskSizes.add(101l * 1024);
        diskSizes.add(101l * 1024);
        diskSizes.add(50l);

        // act
        analyser.acceptHeartbeat(heartbeat, false);

        // assert
        PowerMockito.verifyStatic();
        System.exit(0);
    }

    @Test
    public void itShouldNotStopPiIfAllDiskSpaceIsGreaterThan100M() {
        // setup
        diskSizes.add(101l * 1024);
        diskSizes.add(101l * 1024);

        // act
        analyser.acceptHeartbeat(heartbeat, false);

        // assert
        PowerMockito.verifyStatic(never());
        System.exit(0);
    }

    @Test
    public void itShouldStopPiIfFileSystemIsReadOnly() {
        // setup
        diskSizes.add(101l * 1024);

        // act
        analyser.acceptHeartbeat(heartbeat, true);

        // assert
        PowerMockito.verifyStatic();
        System.exit(0);
    }
}

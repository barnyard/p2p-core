/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class NodePhysicalHealthCheckerTest {
    private static final String NODE_ID = "nodeId";
    private String HOSTNAME;

    @Mock
    private Id id;
    @Mock
    private CommandRunner commandRunner;
    private Queue<List<String>> fileLinesQueue;
    private Collection<NodeHandle> leafset;
    @Mock
    private NodeHandle node;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private ReportingApplication reportingApplication;
    @Mock
    private NodePhysicalHealthAnalyser analyser;
    @Mock
    private IPMIEventLogHeartBeatFiller ipmiEventLogHeartBeatFiller;
    @Mock
    private DiskspaceHeartBeatFiller diskspaceHeartBeatFiller;

    private NodePhysicalHealthChecker checker;

    @Before
    public void doBefore() throws UnknownHostException {
        HOSTNAME = InetAddress.getLocalHost().getHostName();

        checker = new NodePhysicalHealthChecker() {
            @Override
            protected List<String> readFile(String filename) {
                if (fileLinesQueue.isEmpty())
                    return Collections.emptyList();
                return fileLinesQueue.poll();
            }
        };

        fileLinesQueue = new LinkedList<List<String>>();
        leafset = Arrays.asList(node);
        when(reportingApplication.getNodeId()).thenReturn(id);
        when(reportingApplication.getLeafNodeHandles()).thenReturn(leafset);
        when(id.toString()).thenReturn(NODE_ID);
        when(id.toStringFull()).thenReturn(NODE_ID);
        when(node.getNodeId()).thenReturn(id);

        addMemoryInfoLines();
        fileLinesQueue.add(Arrays.asList(new String[] { "0.10 0.20 0.30 1/274 27519" }));
        addNetworkInfoLines();

        when(commandRunner.run(isA(String.class))).thenReturn(new CommandResult(0, new ArrayList<String>(), null));
        when(commandRunner.runInShell(isA(String.class))).thenReturn(new CommandResult(0, new ArrayList<String>(), null));

        checker.setAnalyser(analyser);
        checker.setScheduledExecutorService(scheduledExecutorService);
        checker.setReportingApplication(reportingApplication);
        checker.setCommandRunner(commandRunner);
        checker.onApplicationEvent(new NodeStartedEvent(this));
        checker.setHeartBeatFillers(Arrays.asList(ipmiEventLogHeartBeatFiller, diskspaceHeartBeatFiller));
    }

    @Test
    public void itShouldPublishHeartbeats() {
        checker.report();

        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new HeartbeatMatcher() {
            @Override
            public boolean alsoMatches(HeartbeatEntity hb) {
                description = "hostname:" + HOSTNAME + ", nodeid:" + NODE_ID;
                if (!hb.getHostname().equals(HOSTNAME)) {
                    return false;
                }
                if (!hb.getNodeId().equals(HOSTNAME))
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void itShouldPassTheHeartbeatToTheAnalyser() {
        // setup

        // act
        checker.report();

        // assert
        verify(analyser).acceptHeartbeat(isA(HeartbeatEntity.class), eq(false));
    }

    @Test
    public void itShouldAddTheMemoryInfoToTheHeartbeat() throws IOException {
        // act
        checker.report();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new HeartbeatMatcher() {
            @Override
            public boolean alsoMatches(HeartbeatEntity hb) {
                if (hb.getMemoryDetails().size() == 0)
                    return false;
                if (hb.getMemoryDetails().get("MemFree") != 1065924l)
                    return false;
                if (hb.getMemoryDetails().get("SwapFree") != 1015400l)
                    return false;
                return true;
            }
        }));
    }

    @Test
    public void itShouldAddTheLeafset() {
        // act
        checker.report();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new HeartbeatMatcher() {
            @Override
            public boolean alsoMatches(HeartbeatEntity hb) {
                if (hb.getLeafSet().size() != 1)
                    return false;
                if (hb.getLeafSet().iterator().next().equals(NODE_ID))
                    return true;
                return false;
            }
        }));
    }

    @Test
    public void itShouldNotReportReadOnlyFileSystemIfCommandFailedForSomeOtherReason() throws Exception {
        // setup
        when(commandRunner.run("touch /tmp/tmp.txt")).thenThrow(new CommandExecutionException());

        // act
        checker.report();

        // assert
        verify(analyser).acceptHeartbeat(isA(HeartbeatEntity.class), eq(false));
    }

    @Test
    public void itShouldReportReadOnlyFileSystemIfRootFileSystemIsReadOnly() {
        // setup
        when(commandRunner.run("touch /tmp/tmp.txt")).thenThrow(new CommandExecutionException("Error executing command", Arrays.asList("touch: cannot touch `a': Read-only file system")));

        // act
        checker.report();

        // assert
        verify(analyser).acceptHeartbeat(isA(HeartbeatEntity.class), eq(true));
    }

    @Test
    public void itShouldReportReadOnlyFileSystemIfOptFileSystemIsReadOnly() {
        // setup
        when(commandRunner.run("touch /opt/pi/var/run/tmp.txt")).thenThrow(new CommandExecutionException("Error executing command", Arrays.asList("touch: cannot touch `a': Read-only file system")));

        // act
        checker.report();

        // assert
        verify(analyser).acceptHeartbeat(isA(HeartbeatEntity.class), eq(true));
    }

    @Test
    public void itShouldReportReadOnlyFileSystemIfRootFileSystemIsOkButOptFileSystemIsReadOnly() {
        // setup
        CommandResult commandResult = mock(CommandResult.class);
        when(commandRunner.run("touch /tmp/txt")).thenReturn(commandResult);
        when(commandRunner.run("touch /opt/pi/var/run/tmp.txt")).thenThrow(new CommandExecutionException("Error executing command", Arrays.asList("touch: cannot touch `a': Read-only file system")));

        // act
        checker.report();

        // assert
        verify(analyser).acceptHeartbeat(isA(HeartbeatEntity.class), eq(true));
    }

    @Test
    public void shouldStopPiIfCommandsFailedForNotAllocatingMemory() {
        // setup
        when(diskspaceHeartBeatFiller.populate(isA(HeartbeatEntity.class))).thenThrow(
                new CommandExecutionException("Error executing command", new IOException("java.io.IOException: Cannot run program \"df\": java.io.IOException: error=12, Cannot allocate memory")));

        // act
        checker.report();

        // assert
        verify(analyser).exitPi("IOException: Cannot allocate memory");
    }

    @Test
    public void shouldNotStopPiIfCommandsFailedForSomeOtherException() {
        // setup
        when(diskspaceHeartBeatFiller.populate(isA(HeartbeatEntity.class))).thenThrow(new CommandExecutionException("Error executing command", new IllegalArgumentException("Cannot run program \"df\": Cannot allocate memory")));

        // act
        checker.report();

        // assert
        verify(analyser, never()).exitPi("IOException: Cannot allocate memory");
    }

    @Test
    public void itShouldPopulateTheHeartbeatUsingTheFillers() {
        // act
        checker.report();

        // assert
        verify(ipmiEventLogHeartBeatFiller).populate(isA(HeartbeatEntity.class));
        verify(diskspaceHeartBeatFiller).populate(isA(HeartbeatEntity.class));
    }

    @Test
    public void shouldSpinOffSchedulerThreadOnPostConstruct() throws Exception {
        // act
        checker.scheduleReportingOfHeartbeats();

        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0l), eq(300l), eq(TimeUnit.SECONDS));
    }

    private void addNetworkInfoLines() {
        List<String> fileLines = new ArrayList<String>();
        fileLines.add("Inter-|   Receive                                                |  Transmit");
        fileLines.add("face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed");
        fileLines.add("lo:5266269191 32904975    1    0    0     0          0         0 5266269191 32904975    5    0    0     0       0          0");
        fileLines.add("peth1:1224566343 1730565    0    0    0     0          0    119079 622750657 1023252    0    0    0     0       0          0");
        fileLines.add("peth0:68579293497 110129356   13    0    0    13          0  11136635 157467843795 129867209    0    0    0     0       0          0");
        fileLines.add("vif0.2:       9       0    0    0    0     0          0         0        2       0    0    0    0     0       0          0");
        fileLinesQueue.add(fileLines);

    }

    private void addMemoryInfoLines() {
        List<String> fileLines = new ArrayList<String>();
        fileLines.add("MemTotal:     16034816 kB");
        fileLines.add("MemFree:       1065924 kB");
        fileLines.add("Buffers:        200016 kB");
        fileLines.add("Cached:       13749824 kB");
        fileLines.add("SwapCached:       4656 kB");
        fileLines.add("Active:        1761684 kB");
        fileLines.add("Inactive:     12293272 kB");
        fileLines.add("HighTotal:           0 kB");
        fileLines.add("HighFree:            0 kB");
        fileLines.add("LowTotal:     16034816 kB");
        fileLines.add("LowFree:       1065924 kB");
        fileLines.add("SwapTotal:     1020116 kB");
        fileLines.add("SwapFree:      1015400 kB");
        fileLines.add("VmallocChunk: 34359735163 kB");
        fileLinesQueue.add(fileLines);
    }

    private class HeartbeatMatcher extends BaseMatcher<HeartbeatEntity> {
        protected String description = "";

        public boolean alsoMatches(HeartbeatEntity heartbeat) {
            return true;
        }

        @Override
        public boolean matches(Object arg0) {
            HeartbeatEntity hb = (HeartbeatEntity) arg0;
            return alsoMatches(hb);
        }

        @Override
        public void describeTo(Description arg0) {
            arg0.appendText(description);
        }
    }
}

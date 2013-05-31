/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import rice.pastry.NodeHandle;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class NodePhysicalHealthChecker implements ApplicationListener<NodeStartedEvent> {
    private static final String READ_ONLY_FILE_SYSTEM = "Read-only file system";
    private static final String READ_ONLY_FILE_SYSTEM_TEST_COMMAND = "touch /tmp/tmp.txt";
    private static final String READ_ONLY_FILE_SYSTEM_TEST_COMMAND_FOR_PI_DIR = "touch /opt/pi/var/run/tmp.txt";
    private static final String MEM_FREE = "MemFree";
    private static final String SWAP_FREE = "SwapFree";
    private static final String COLON = ":";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final String UNABLE_TO_READ_FILE = "Unable to read file ";
    private static final Log LOG = LogFactory.getLog(NodePhysicalHealthChecker.class);
    private static final String REPORT_DELAYS_SECONDS = "300";
    private long publishIntervalSeconds;
    private ScheduledExecutorService scheduledExecutorService;
    private ReportingApplication reportingApplication;
    private NodePhysicalHealthAnalyser analyser;
    private AtomicBoolean nodeStarted;
    private CommandRunner commandRunner;
    private List<HeartBeatFiller> heartBeatFillers;
    private String commandToTestReadOnlyFileSystemInPiDirectory;

    public NodePhysicalHealthChecker() {
        publishIntervalSeconds = Integer.parseInt(REPORT_DELAYS_SECONDS);
        scheduledExecutorService = null;
        reportingApplication = null;
        analyser = null;
        commandRunner = null;
        nodeStarted = new AtomicBoolean(false);
        heartBeatFillers = null;
        commandToTestReadOnlyFileSystemInPiDirectory = READ_ONLY_FILE_SYSTEM_TEST_COMMAND_FOR_PI_DIR;
    }

    @Property(key = "health.publishintervalsize", defaultValue = REPORT_DELAYS_SECONDS)
    public void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        LOG.debug(String.format("setPublishIntervalSeconds(%d)", aPublishIntervalSeconds));
        publishIntervalSeconds = aPublishIntervalSeconds;
    }

    @Property(key = "check.readonly.filesystem.pi.directory", defaultValue = READ_ONLY_FILE_SYSTEM_TEST_COMMAND_FOR_PI_DIR)
    public void setCommandToCheckForReadOnlyFileSystemInPiDirectory(String directory) {
        commandToTestReadOnlyFileSystemInPiDirectory = directory;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Resource
    public void setReportingApplication(ReportingApplication aReportingApplication) {
        reportingApplication = aReportingApplication;
    }

    @Resource
    public void setAnalyser(NodePhysicalHealthAnalyser theAnalyser) {
        analyser = theAnalyser;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        commandRunner = aCommandRunner;
    }

    @Resource
    public void setHeartBeatFillers(List<HeartBeatFiller> theHeartBeatFillers) {
        this.heartBeatFillers = theHeartBeatFillers;
    }

    @PostConstruct
    public void scheduleReportingOfHeartbeats() {
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                report();
            }
        }, 0, publishIntervalSeconds, TimeUnit.SECONDS);
    }

    protected String getHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    public void report() {
        LOG.debug(String.format("report(), nodeStarted = %s", nodeStarted.get()));
        if (nodeStarted.get()) {
            try {
                final HeartbeatEntity heartbeat = new HeartbeatEntity(getHostname());

                populateLeafSet(heartbeat);
                populateMemoryInfo(heartbeat);
                for (HeartBeatFiller heartBeatFiller : heartBeatFillers) {
                    heartBeatFiller.populate(heartbeat);
                }
                analyser.acceptHeartbeat(heartbeat, isFileSystemReadOnly());
                reportingApplication.sendReportingUpdateToASuperNode(heartbeat);
            } catch (UnknownHostException e) {
                LOG.error("Curiously, the health checker was unable to get the local hostname. Is the network OK?", e);
            } catch (CommandExecutionException cex) {
                if (errorContains(cex, "Cannot allocate memory")) {
                    analyser.exitPi("IOException: Cannot allocate memory");
                }
            } catch (Exception e) {
                LOG.error("Exception while running heatbeat.", e);
            }
        }
    }

    private void populateLeafSet(HeartbeatEntity heartbeat) {
        for (NodeHandle node : reportingApplication.getLeafNodeHandles()) {
            heartbeat.getLeafSet().add(node.getNodeId().toString());
        }
    }

    @SuppressWarnings("unchecked")
    protected List<String> readFile(String filename) {
        try {
            return FileUtils.readLines(new File(filename));
        } catch (IOException e) {
            LOG.error(UNABLE_TO_READ_FILE + filename, e);
        }
        return Collections.emptyList();
    }

    private void populateMemoryInfo(HeartbeatEntity heartbeat) {
        Map<String, Long> meminfo = heartbeat.getMemoryDetails();
        for (String line : readFile("/proc/meminfo")) {
            if (line.contains(SWAP_FREE) || line.contains(MEM_FREE)) {
                String[] words = line.split(WHITESPACE_REGEX);
                meminfo.put(words[0].replace(COLON, ""), Long.parseLong(words[1]));
            }
        }
    }

    private boolean isFileSystemReadOnly() {
        try {
            commandRunner.run(READ_ONLY_FILE_SYSTEM_TEST_COMMAND);
            commandRunner.run(commandToTestReadOnlyFileSystemInPiDirectory);
        } catch (CommandExecutionException cex) {
            LOG.error(String.format("Command: %s or %s failed.", READ_ONLY_FILE_SYSTEM_TEST_COMMAND, commandToTestReadOnlyFileSystemInPiDirectory));

            if (errorContains(cex, READ_ONLY_FILE_SYSTEM)) {
                return true;
            }
        }

        return false;
    }

    private boolean errorContains(CommandExecutionException cex, String message) {
        // check cause and message
        if (cex.getCause() != null && (cex.getCause() instanceof IOException) && (cex.getCause().getMessage().contains(message)))
            return true;

        List<String> errorLines = cex.getErrorLines();

        if (null != errorLines) {
            for (String errorLine : errorLines) {
                if (errorLine.contains(message))
                    return true;
            }
        }

        return false;
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        LOG.debug(String.format("onApplicationEvent(%s)", event));
        nodeStarted.set(true);
    }
}

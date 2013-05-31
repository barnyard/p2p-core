/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.conf.Property;

@Component
public class NodePhysicalHealthAnalyser {
    private static final int ONE_HUNDRED = 100;

    private static final long ONE_THOUSAND_TWENTY_FOUR = 1024L;

    private static final Log LOG = LogFactory.getLog(NodePhysicalHealthAnalyser.class);

    private HeartbeatEntity lastHeartbeat;
    private long minimumDiskSpace = ONE_HUNDRED * ONE_THOUSAND_TWENTY_FOUR;

    public NodePhysicalHealthAnalyser() {
        lastHeartbeat = null;
    }

    @Property(key = "health.analyser.minimum.acceptable.diskspace.mb", defaultValue = "100")
    public void setMinimumAcceptableDiskspaceMB(int megabytes) {
        minimumDiskSpace = megabytes * ONE_THOUSAND_TWENTY_FOUR;
    }

    public void acceptHeartbeat(HeartbeatEntity heartbeat, boolean isFileSystemReadOnly) {
        if (isFileSystemReadOnly) {
            exitPi("Readonly file system encountered while writing out to temp file");
        }

        if (lastHeartbeat != null) {
            if (heartbeat.getLeafSet().isEmpty() && !lastHeartbeat.getLeafSet().isEmpty()) {
                exitPi("No nodes found in leafset.");
            }
        }

        for (Long space : heartbeat.getDiskSpace().values()) {
            if (space < minimumDiskSpace)
                exitPi("About to run out of disk space.");
        }

        lastHeartbeat = heartbeat;
    }

    public void exitPi(String logMessage) {
        try {
            LOG.fatal("!!!!!!!!!!!!!!!!!!!!!!   Pi is shutting down to avoid damage to the system. !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + logMessage);
        } finally {
            System.exit(0);
        }
    }

}

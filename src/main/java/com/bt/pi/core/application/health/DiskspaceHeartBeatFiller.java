/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class DiskspaceHeartBeatFiller implements HeartBeatFiller {
    private static final int DISK_NAME_POSITION = 0;
    private static final int DISK_SPACE_AVAILABLE_POSITION = 3;

    private CommandRunner commandRunner;

    public DiskspaceHeartBeatFiller() {
        commandRunner = null;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        commandRunner = aCommandRunner;
    }

    @Override
    public HeartbeatEntity populate(HeartbeatEntity heartbeat) {
        CommandResult commandResult = commandRunner.run("df -lx tmpfs");
        if (commandResult.getReturnCode() == 0) {
            for (String line : commandResult.getOutputLines()) {
                if (line.matches("^/dev/.*")) {
                    String[] words = line.split("\\s+");
                    heartbeat.getDiskSpace().put(words[DISK_NAME_POSITION], Long.parseLong(words[DISK_SPACE_AVAILABLE_POSITION]));
                }
            }
        }

        return heartbeat;
    }

}

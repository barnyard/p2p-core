/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class IPMIEventLogHeartBeatFiller implements HeartBeatFiller {
    private static final int FOUR = 4;
    private static final String IPMITOOL = "ipmitool";
    private static final String SEL = "sel";
    private static final int SEL_DATE_POSITION = 1;
    private static final int SEL_TIME_POSITION = 2;
    private static final int SEL_SENSOR_POSITION = 3;
    private static final int SEL_DESCRIPTION_POSITION = 4;

    private static final Log LOG = LogFactory.getLog(IPMIEventLogHeartBeatFiller.class);

    private CommandRunner commandRunner;

    public IPMIEventLogHeartBeatFiller() {
        commandRunner = null;
    }

    @Resource
    public void setCommandRunner(CommandRunner aCommandRunner) {
        commandRunner = aCommandRunner;
    }

    @Override
    public HeartbeatEntity populate(HeartbeatEntity heartbeat) {
        CommandResult commandResult = commandRunner.run(String.format("%s %s list", IPMITOOL, SEL));
        if (commandResult.getReturnCode() != 0) {
            return heartbeat;
        }
        if (commandResult.getOutputLines().size() == 1 && commandResult.getOutputLines().get(0).equals("SEL has no entries")) {
            return heartbeat;
        }
        // get the events from the SEL
        for (String line : commandResult.getOutputLines()) {
            String[] words = line.split("\\s+\\|\\s+");
            LogMessageEntity event = new LogMessageEntity(heartbeat.getNodeId());
            if (words.length > FOUR) {
                event.setClassName(words[SEL_SENSOR_POSITION]);
                event.setLogMessage(words[SEL_DESCRIPTION_POSITION]);
                String date = words[SEL_DATE_POSITION] + " " + words[SEL_TIME_POSITION];
                try {
                    event.setTimestamp(DateFormat.getDateTimeInstance().format(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse(date)));
                } catch (ParseException e) {
                    LOG.error(String.format("Error parsing date: %s", date), e);
                }
            } else if (words.length > 1) {
                event.setLogMessage(Arrays.toString(Arrays.copyOfRange(words, 1, words.length)));
            }
            heartbeat.getIPMIEvents().add(event);
        }

        // remove the events and clear down the SEL
        commandRunner.run(String.format("%s %s clear", IPMITOOL, SEL));

        return heartbeat;
    }
}

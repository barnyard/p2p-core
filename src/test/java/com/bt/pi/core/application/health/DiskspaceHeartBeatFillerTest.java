/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class DiskspaceHeartBeatFillerTest {
    private HeartbeatEntity heartbeat;
    @Mock
    private CommandRunner commandRunner;
    private DiskspaceHeartBeatFiller filler;

    @Before
    public void doBefore() {
        heartbeat = new HeartbeatEntity("");
        heartbeat.setIPMIEvents(new ArrayList<LogMessageEntity>());
        heartbeat.setDiskSpace(new HashMap<String, Long>());

        filler = new DiskspaceHeartBeatFiller();
        filler.setCommandRunner(commandRunner);
    }

    @Test
    public void itShouldNotAddDiskSpaceIfDfFails() throws IOException, InterruptedException {
        // setup
        when(commandRunner.run("df -lx tmpfs")).thenReturn(new CommandResult(1, new ArrayList<String>(), null));

        // act
        HeartbeatEntity hb = filler.populate(heartbeat);

        assertEquals(0, hb.getDiskSpace().size());
    }

    @Test
    public void itShouldGetTheDiskSpace() throws IOException, InterruptedException {
        // setup
        when(commandRunner.run("df -lx tmpfs")).thenReturn(
                new CommandResult(0, Arrays.asList(new String[] { "Filesystem           1K-blocks      Used Available Use% Mounted on", "/dev/sda1             15872604   3745344  11307948  25% /",
                        "/dev/sda5            925334428 191172360 686399220  22% /state/partition1", "/dev/sda2              3968124    561408   3201888  15% /var" }), null));

        // act
        HeartbeatEntity hb = filler.populate(heartbeat);

        // assert
        assertEquals(3, hb.getDiskSpace().size());
        assertEquals((Long) 11307948l, ((Long) hb.getDiskSpace().get("/dev/sda1")));
        assertEquals((Long) 3201888l, ((Long) hb.getDiskSpace().get("/dev/sda2")));
        assertEquals((Long) 686399220l, ((Long) hb.getDiskSpace().get("/dev/sda5")));
    }
}

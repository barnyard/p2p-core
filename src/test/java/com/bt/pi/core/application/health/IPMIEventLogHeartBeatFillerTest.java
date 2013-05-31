/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.application.health;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

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
public class IPMIEventLogHeartBeatFillerTest {
    private HeartbeatEntity heartbeat;
    @Mock
    private CommandRunner commandRunner;
    private IPMIEventLogHeartBeatFiller filler;

    @Before
    public void doBefore() {
        heartbeat = new HeartbeatEntity("");
        heartbeat.setIPMIEvents(new ArrayList<LogMessageEntity>());

        filler = new IPMIEventLogHeartBeatFiller();
        filler.setCommandRunner(commandRunner);
    }

    @Test
    public void itShouldGetTheSELOverIPMI() throws IOException, InterruptedException, ParseException {
        // setup
        when(commandRunner.run("ipmitool sel list")).thenReturn(
                new CommandResult(0, Arrays.asList(new String[] { "        1 | 07/19/2010 | 09:51:27 | Temperature #0x30 | Upper Critical going high", "        2 | 07/19/2010 | 09:52:40 | Temperature #0x30 | Upper Critical going high",
                        "        3 | 07/19/2010 | 09:52:43 | Voltage #0x60 | Lower Critical going low" }), null));

        // act
        HeartbeatEntity hb = filler.populate(heartbeat);

        // assert
        assertEquals(3, hb.getIPMIEvents().size());
        LogMessageEntity[] events = hb.getIPMIEvents().toArray(new LogMessageEntity[3]);
        assertEquals("Upper Critical going high", events[0].getLogMessage());
        assertEquals("Lower Critical going low", events[2].getLogMessage());
        assertEquals("Temperature #0x30", events[0].getClassName());
        assertEquals(new SimpleDateFormat("dd-MM-yy hh:mm:ss").parse("19-07-10 09:51:27"), DateFormat.getDateTimeInstance().parse(events[0].getTimestamp()));
    }

    @Test
    public void itShouldGetTheSELOverIPMIWithShortLines() throws IOException, InterruptedException, ParseException {
        // setup
        when(commandRunner.run("ipmitool sel list")).thenReturn(
                new CommandResult(0, Arrays
                        .asList(new String[] { "1 | 10/11/2010 | 10:57:17 | Fan #0x66 | Lower Critical going low", "2 | 10/11/2010 | 10:57:19 | Fan #0x67 | Lower Critical going low",
                                "3 | 10/11/2010 | 10:57:20 | Fan #0x68 | Lower Critical going low", "4 | 10/11/2010 | 10:57:22 | Processor #0xc0 | Presence detected | Asserted",
                                "5 | 10/11/2010 | 10:57:23 | Processor #0xc1 | Presence detected | Asserted", "6 | 10/11/2010 | 11:00:15 | OS Stop/Shutdown #0x56 | Run-time critical stop | Asserted", "7 | Linux kernel panic: VFS: Unable",
                                "8 | Linux kernel panic: to mount r | blah blah", "9 | Linux kernel panic: oot fs on u", "a | Linux kernel panic: nknown-bloc", "b | Linux kernel panic: k(9,0)",
                                "c | 10/11/2010 | 11:06:43 | Power Unit #0xc9 | Power off/down | Asserted", "d | 10/11/2010 | 11:06:53 | Power Unit #0xc9 | Power off/down | Deasserted" }), null));

        // act
        HeartbeatEntity hb = filler.populate(heartbeat);

        // assert
        LogMessageEntity[] events = hb.getIPMIEvents().toArray(new LogMessageEntity[hb.getIPMIEvents().size()]);
        assertEquals(13, events.length);
        assertEquals("Lower Critical going low", events[0].getLogMessage());
        assertEquals("Fan #0x66", events[0].getClassName());
        assertEquals("Lower Critical going low", events[1].getLogMessage());
        assertEquals("Fan #0x67", events[1].getClassName());
        assertEquals("Lower Critical going low", events[2].getLogMessage());
        assertEquals("Fan #0x68", events[2].getClassName());
        assertEquals("Presence detected", events[3].getLogMessage());
        assertEquals("Processor #0xc0", events[3].getClassName());
        assertEquals("Presence detected", events[4].getLogMessage());
        assertEquals("Processor #0xc1", events[4].getClassName());
        assertEquals("Run-time critical stop", events[5].getLogMessage());
        assertEquals("OS Stop/Shutdown #0x56", events[5].getClassName());

        assertEquals("[Linux kernel panic: VFS: Unable]", events[6].getLogMessage());
        assertEquals("[Linux kernel panic: to mount r, blah blah]", events[7].getLogMessage());
        assertEquals("[Linux kernel panic: oot fs on u]", events[8].getLogMessage());
        assertEquals("[Linux kernel panic: nknown-bloc]", events[9].getLogMessage());
        assertEquals("[Linux kernel panic: k(9,0)]", events[10].getLogMessage());

        assertEquals("Power off/down", events[11].getLogMessage());
        assertEquals("Power Unit #0xc9", events[11].getClassName());
        assertEquals("Power off/down", events[12].getLogMessage());
        assertEquals("Power Unit #0xc9", events[12].getClassName());

        assertEquals(new SimpleDateFormat("dd-MM-yy hh:mm:ss").parse("11-10-10 10:57:17"), DateFormat.getDateTimeInstance().parse(events[0].getTimestamp()));
    }

    @Test
    public void itShouldNotAddIPMIEventsWhenThereAreNone() throws IOException, InterruptedException {
        // setup
        when(commandRunner.run("ipmitool sel list")).thenReturn(new CommandResult(0, Arrays.asList(new String[] { "SEL has no entries" }), null));

        // act
        HeartbeatEntity hb = filler.populate(heartbeat);

        // assert
        assertEquals(0, hb.getIPMIEvents().size());
    }
}

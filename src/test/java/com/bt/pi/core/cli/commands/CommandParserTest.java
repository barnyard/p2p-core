package com.bt.pi.core.cli.commands;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.commands.Command;
import com.bt.pi.core.cli.commands.CommandBase;
import com.bt.pi.core.cli.commands.CommandParseException;
import com.bt.pi.core.cli.commands.CommandParser;
import com.bt.pi.core.cli.commands.ManagementCommand;

public class CommandParserTest {
    private CommandParser commandParser;
    private Command command;

    @Before
    public void before() {
        command = new CommandBase() {
            public void execute(PrintStream outputStream) {
            }

            public String getDescription() {
                return "test command";
            }

            public String getKeyword() {
                return "test";
            }
        };

        commandParser = new CommandParser() {
            @Override
            protected void populateCommandMap() {
                super.populateCommandMap();
                getCommandMap().put("test", command);
            }
        };
        commandParser.setExternalManagementCommands(new ArrayList<ManagementCommand>());
    }

    /**
     * Should barf if command unknown
     */
    @Test(expected = CommandParseException.class)
    public void shouldBarfIfCommandUnknown() {
        // act
        commandParser.parse("huh");
    }

    /**
     * Should return recognised command
     */
    @Test
    public void shouldReturnRecognisedCommand() {
        // act
        Command command = commandParser.parse("test");

        // assert
        assertEquals(this.command, command);
    }

    /**
     * Should return recognised command after trimming
     */
    @Test
    public void shouldReturnRecognisedCommandAfterTrimming() {
        // act
        Command command = commandParser.parse("    test  ");

        // assert
        assertEquals(this.command, command);
    }

    /**
     * Should return recognised command after trimming with args
     */
    @Test
    public void shouldReturnRecognisedCommandAfterTrimmingWithArgs() {
        // act
        Command command = commandParser.parse("    test  one-arg twoargs   threeArgs   ");

        // assert
        assertEquals(this.command, command);
        assertEquals(3, command.getArgs().length);
        assertEquals("one-arg", command.getArgs()[0]);
        assertEquals("twoargs", command.getArgs()[1]);
        assertEquals("threeArgs", command.getArgs()[2]);
    }

    /**
     * Should return help command
     */
    @Test
    public void shouldReturnHelpCommand() {
        // WTF ?
    }
}
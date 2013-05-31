package com.bt.pi.core.util.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.commands.CommandExecutionException;

public class CommandExecutionExceptionTest {
    private CommandExecutionException commandExecutionException;

    @Before
    public void before() {
        commandExecutionException = new CommandExecutionException("oops", Arrays.asList(new String[] { "boom", "bang" }));
    }

    @Test
    public void shouldBeAbleToGetErrorLinesFromException() {
        // act
        List<String> res = commandExecutionException.getErrorLines();

        // assert
        assertTrue(res.contains("boom"));
        assertTrue(res.contains("bang"));
    }

    @Test
    public void shouldHaveNonNullErrorLinesForExceptionWhereErrorLinesNotGiven() {
        // setup
        commandExecutionException = new CommandExecutionException("oops", new RuntimeException("ouch"));

        // act
        List<String> res = commandExecutionException.getErrorLines();

        // assert
        assertEquals(0, res.size());
    }
}

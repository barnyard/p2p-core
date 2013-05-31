package com.bt.pi.core.util.common;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import com.bt.pi.core.util.common.CommandLineParser;

public class CommandLineParserTest {

    private final CommandLineParser parser = new CommandLineParser();

    @Test
    public void testSimpleCase() {
        String[] commands = parser.parse("ps -ef");
        assertArrayEquals(new String[] { "ps", "-ef" }, commands);
    }

    @Test
    public void testQuotedArgument() {
        String[] commands = parser.parse("sh -c 'ps -ef | grep java'");
        assertArrayEquals(new String[] { "sh", "-c", "ps -ef | grep java" }, commands);
    }

    @Test
    public void testDoubleEscapedQuotesInQuotedArgument() {
        String[] commands = parser.parse("sh -c 'echo ''hello'''");
        assertArrayEquals(new String[] { "sh", "-c", "echo 'hello'" }, commands);
    }

    @Test
    public void testDoubleDoubleEscapedQuotesAfterSpace() {
        String[] commands = parser.parse("echo ''''hello''");
        assertArrayEquals(new String[] { "echo", "'hello'" }, commands);
    }

}

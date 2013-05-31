package com.bt.pi.core.util.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executor;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.ragstorooks.testrr.cli.CommandExecutor;

public class CommandRunnerTest {
    private static final String NICELY_COMMAND_PREFIX = "nice -n+10 ionice -c3";
    private CommandRunner commandRunner;
    private CommandExecutor commandExecutor;
    private CommandLineParser commandLineParser;
    private String commandLine = "a b c d e";
    private String runInShellCommand;
    private List<String> errorLines;

    @Before
    public void before() {
        errorLines = Arrays.asList(new String[] { "bang", "boom", "crash" });

        commandExecutor = mock(CommandExecutor.class);

        this.commandRunner = new CommandRunner() {
            @Override
            protected CommandExecutor createCommandExecutor() {
                return commandExecutor;
            }
        };

        this.commandRunner.setExecutor(new Executor() {
            @Override
            public void execute(Runnable runnable) {
                runnable.run();
            }
        });

        commandLineParser = mock(CommandLineParser.class);

        commandRunner.setCommandLineParser(commandLineParser);
        commandRunner.setNicelyCommandPrefix(NICELY_COMMAND_PREFIX);

        when(commandLineParser.parse(commandLine)).thenReturn(new String[] { "a", "b", "c", "d", "e" });
    }

    @Test
    public void testRun() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String[] commands = (String[]) invocation.getArguments()[0];
                assertEquals("[a, b, c, d, e]", Arrays.toString(commands));
                return 0;
            }
        });

        // act
        this.commandRunner.run(commandLine);

        // assert
        verify(this.commandExecutor).executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false));
    }

    @Test(expected = CommandExecutionException.class)
    public void testRunNonZero() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String[] commands = (String[]) invocation.getArguments()[0];
                assertEquals("[a, b, c, d, e]", Arrays.toString(commands));
                return 1;
            }
        });
        when(this.commandExecutor.getErrorLines()).thenReturn(errorLines);

        // act
        try {
            this.commandRunner.run(commandLine);
            fail("expected RuntimeException");
        } catch (CommandExecutionException e) {
            assertEquals("Error executing " + commandLine + ", return code: 1", e.getMessage());
            assertEquals(errorLines, e.getErrorLines());
            throw e;
        }
    }

    @Test(expected = CommandExecutionException.class)
    public void testRunThrowsIOException() throws Exception {
        // setup
        String message = "shit happens";
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false))).thenThrow(new IOException(message));

        // act
        try {
            this.commandRunner.run(commandLine);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Error executing " + commandLine, e.getMessage());
            assertTrue(e.getCause() instanceof IOException);
            assertEquals(message, e.getCause().getMessage());
            throw e;
        }
    }

    @Test(expected = CommandExecutionException.class)
    public void testRunThrowsInteruptedException() throws Exception {
        // setup
        String message = "shit happens";
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false))).thenThrow(new InterruptedException(message));

        // act
        try {
            this.commandRunner.run(commandLine);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Error executing " + commandLine, e.getMessage());
            assertTrue(e.getCause() instanceof InterruptedException);
            assertEquals(message, e.getCause().getMessage());
            throw e;
        }
    }

    @Test
    public void testRunInShell() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String[] commands = (String[]) invocation.getArguments()[0];
                assertEquals("[/bin/sh, -c, a b c d e]", Arrays.toString(commands));
                return 0;
            }
        });

        // act
        this.commandRunner.runInShell(commandLine);

        // assert
        verify(this.commandExecutor).executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false));
    }

    @Test
    public void testRunNicely() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String[] commands = (String[]) invocation.getArguments()[0];
                assertEquals("[nice, -n+10, ionice, -c3, a, b, c, d, e]", Arrays.toString(commands));
                return 0;
            }
        });

        when(commandLineParser.parse("nice -n+10 ionice -c3 " + commandLine)).thenReturn(new String[] { "nice", "-n+10", "ionice", "-c3", "a", "b", "c", "d", "e" });

        // act
        this.commandRunner.runNicely(commandLine);

        // assert
        verify(this.commandExecutor).executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L), eq(false));
    }

    @Test
    public void testRunNicelyInShell() throws Exception {
        // setup

        commandRunner = new CommandRunner() {
            public CommandResult runInShell(String command) {
                runInShellCommand = command;
                return null;
            }
        };
        commandRunner.setNicelyCommandPrefix(NICELY_COMMAND_PREFIX);

        // act
        this.commandRunner.runNicelyInShell(commandLine);

        // assert
        assertEquals(String.format("%s %s", NICELY_COMMAND_PREFIX, commandLine), runInShellCommand);
    }

    class TestAppender extends AppenderSkeleton {

        private List<String> errorLines;
        private List<String> debugLines;

        public TestAppender() {
            this.errorLines = new Vector<String>();
            this.debugLines = new Vector<String>();
        }

        public List<String> getErrorLines() {
            return this.errorLines;
        }

        public List<String> getDebugLines() {
            return this.debugLines;
        }

        @Override
        protected void append(LoggingEvent arg0) {
            if (arg0.getLevel().equals(Level.ERROR))
                this.errorLines.add(arg0.getMessage().toString());
            if (arg0.getLevel().equals(Level.DEBUG))
                this.debugLines.add(arg0.getMessage().toString());
        }

        @Override
        public void close() {
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

    @Test
    public void testThatErrorsFromCommandExecutorAreNotLoggedToErrorWithZeroReturnCode() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false))).thenReturn(0);
        List<String> errorLines = Arrays.asList(new String[] { "shit", "happens" });
        when(this.commandExecutor.getErrorLines()).thenReturn(errorLines);
        Logger logger = Logger.getLogger(CommandRunner.class);
        logger.removeAllAppenders();
        TestAppender testAppender = new TestAppender();
        logger.addAppender(testAppender);

        // act
        CommandResult result = this.commandRunner.run(commandLine);

        // assert
        assertEquals(0, result.getReturnCode());
        assertEquals(errorLines, result.getErrorLines());
        assertFalse(testAppender.getErrorLines().contains("shit"));
        assertFalse(testAppender.getErrorLines().contains("happens"));
    }

    @Test
    public void testThatOutputLinesGetLogged() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(-1L))).thenReturn(0);
        List<String> outputLines = Arrays.asList(new String[] { "some output", "some more output" });
        when(this.commandExecutor.getOutputLines()).thenReturn(outputLines);
        Logger logger = Logger.getLogger(CommandRunner.class);
        logger.removeAllAppenders();
        TestAppender testAppender = new TestAppender();
        logger.addAppender(testAppender);

        // act
        CommandResult result = commandRunner.run(commandLine);

        // assert
        assertEquals(0, result.getReturnCode());
        assertEquals(outputLines, result.getOutputLines());
        assertTrue(testAppender.getDebugLines().contains("some output"));
        assertTrue(testAppender.getDebugLines().contains("some more output"));
    }

    @Test
    public void testThatWeGetResponsesEvenThoughTheProcessTakesTooLong() throws Exception {
        // setup
        when(this.commandExecutor.executeScript((String[]) anyObject(), isA(Runtime.class), eq(false), eq(100L), eq(false))).thenReturn(-1);
        List<String> outputLines = Arrays.asList(new String[] { "some output", "some more output" });
        when(this.commandExecutor.getOutputLines()).thenReturn(outputLines);
        // act
        CommandResult result = commandRunner.run(commandLine, 100L);

        // assert
        assertEquals(-1, result.getReturnCode());
        assertEquals(outputLines, result.getOutputLines());
    }
}

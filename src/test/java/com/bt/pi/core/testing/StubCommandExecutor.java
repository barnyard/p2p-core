package com.bt.pi.core.testing;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ragstorooks.testrr.cli.CommandExecutor;

public class StubCommandExecutor extends CommandExecutor {
    private static final Log LOG = LogFactory.getLog(StubCommandExecutor.class);
    private Collection<String[]> executeCommands;

    public StubCommandExecutor(Executor executor) {
        super(executor);
        executeCommands = Collections.synchronizedList(new ArrayList<String[]>());
    }

    public void clearCommands() {
        executeCommands.clear();
    }

    public boolean assertCommand(String[] arrayTarget) {
        String target = Arrays.toString(arrayTarget);
        List<String> executedCommandsToLog = new ArrayList<String>();
        synchronized (executeCommands) {
            for (String[] command : executeCommands) {
                System.err.println("checking against command: " + Arrays.toString(command) + " looking for: " + target);
                if (target.equals(Arrays.toString(command)))
                    return true;
                executedCommandsToLog.add(Arrays.toString(command));
            }
        }
        System.err.println(String.format("Command %s not found in stub command executor lines %s", target, executedCommandsToLog));
        return false;
    }

    public boolean assertCommandSilently(String[] arrayTarget) {
        String target = Arrays.toString(arrayTarget);
        List<String> executedCommandsToLog = new ArrayList<String>();
        synchronized (executeCommands) {
            for (String[] command : executeCommands) {

                if (target.equals(Arrays.toString(command)))
                    return true;
                executedCommandsToLog.add(Arrays.toString(command));
            }
        }
        System.err.println(String.format("Command %s not found in stub command executor lines %s", target, executedCommandsToLog));
        return false;
    }

    public boolean assertCommand(String[] arrayTarget, int occurences) {
        String target = Arrays.toString(arrayTarget);
        int count = 0;
        synchronized (executeCommands) {
            for (String[] command : executeCommands) {
                System.err.println("checking against command: " + Arrays.toString(command) + " looking for: " + target);
                if (target.equals(Arrays.toString(command)))
                    count++;
            }
        }
        return (count >= occurences);
    }

    public boolean assertThatEmbedKeyCommandIsRun(String instanceImagePath) {
        synchronized (executeCommands) {
            for (String[] command : executeCommands) {
                System.err.println(ArrayUtils.toString(command));
                if (command.length > 0 && command.length == 3) {
                    if (!command[0].equals("/opt/pi/current/bin/add_key.sh"))
                        continue;
                    if (!command[1].equals(instanceImagePath))
                        continue;
                    if (!command[2].startsWith(System.getProperty("java.io.tmpdir")))
                        continue;
                    if (!command[2].contains("sckey"))
                        continue;
                    if (!command[2].endsWith(".tmp"))
                        continue;
                    return true;
                }
            }
        }
        System.err.println("instance path: " + instanceImagePath);
        System.err.println("tempdir " + System.getProperty("java.io.tmpdir"));
        synchronized (executeCommands) {
            for (String[] str : executeCommands)
                System.err.println("looking for key: " + ArrayUtils.toString(str));
        }
        System.err.println("add key command not found in list of executed commands");
        return false;
    }

    public boolean assertCommandMissing(String[] arrayTarget) {
        String target = Arrays.toString(arrayTarget);
        synchronized (executeCommands) {
            for (String[] command : executeCommands) {
                if (target.equals(Arrays.toString(command))) {
                    System.err.println(String.format("Command %s should not run", target));
                    return false;
                }
            }
        }
        return true;
    }

    public void logCommandToStream(PrintStream printStream, String[] command) {
        printStream.println(Arrays.asList(command));
    }

    public void logCommandsToStream(PrintStream printStream) {
        printStream.println("List of executed commands:");
        synchronized (executeCommands) {
            for (String[] command : executeCommands)
                logCommandToStream(printStream, command);
        }
        printStream.println();
    }

    public Collection<String[]> getCommands() {
        return executeCommands;
    }

    @Override
    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors, long maxWaitTime, boolean shouldShutdown) throws IOException, InterruptedException {
        LOG.debug(String.format("executeScript(%s, %s, %s, %s, %s)", mdcMap, Arrays.toString(command), runtime, logErrors, maxWaitTime, shouldShutdown));
        executeCommands.add(command);
        return 0;
    }

    @Override
    public int executeScript(String[] command, Runtime runtime) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, true, -1L);
    }

    @Override
    public int executeScript(String[] command, Runtime runtime, long maxWaitTime) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, true, maxWaitTime);
    }

    @Override
    public int executeScript(String[] command, Runtime runtime, boolean logErrors) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, logErrors, -1L);
    }

    @Override
    public int executeScript(String[] command, Runtime runtime, boolean logErrors, long maxWaitTime) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, logErrors, maxWaitTime);
    }

    @Override
    public int executeScript(Map<String, Object> mdcMap, String[] command, Runtime runtime, boolean logErrors) throws IOException, InterruptedException {
        return executeScript(mdcMap, command, runtime, logErrors, -1L);
    }

    @Override
    public int executeScript(String[] command, Runtime runtime, boolean logErrors, long maxWaitTime, boolean stopOnShutdown) throws IOException, InterruptedException {
        return executeScript(null, command, runtime, logErrors, maxWaitTime, stopOnShutdown);
    }

    @Override
    public List<String> getOutputLines() {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getErrorLines() {
        return new ArrayList<String>();
    }
}

/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.util.common;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.conf.Property;
import com.ragstorooks.testrr.cli.CommandExecutor;

@Component
public class CommandRunner {
    public static final int NO_MAX_WAIT_TIME = -1;
    public static final boolean DEFAULT_SHOULD_SHUTDOWN_PROCESS_ON_APP_EXIT = false;
    private static final String DASH_C = "-c";
    private static final String BIN_SH = "/bin/sh";
    private static final Log LOG = LogFactory.getLog(CommandRunner.class);
    private static final String S_S = "%s %s";
    private static final boolean LOG_CLI_ERROR_LINES = false;
    private Executor executor;
    private CommandLineParser commandLineParser;
    private String nicelyCommandPrefix;

    public CommandRunner() {
        executor = null;
        commandLineParser = null;
    }

    @Resource(type = ThreadPoolTaskExecutor.class)
    public void setExecutor(Executor anExecutor) {
        this.executor = anExecutor;
    }

    @Resource
    public void setCommandLineParser(CommandLineParser aCommandLineParser) {
        commandLineParser = aCommandLineParser;
    }

    @Property(key = "nicely.command.prefix", defaultValue = "nice -n +10 ionice -c3")
    public void setNicelyCommandPrefix(String theNicelyCommandPrefix) {
        nicelyCommandPrefix = theNicelyCommandPrefix;
    }

    public CommandResult run(String commandLine) {
        return run(commandLineParser.parse(commandLine));
    }

    public CommandResult run(String commandLine, long maxWaitTime) {
        return run(commandLineParser.parse(commandLine), maxWaitTime, DEFAULT_SHOULD_SHUTDOWN_PROCESS_ON_APP_EXIT);
    }

    public CommandResult run(String commandLine, long maxWaitTime, boolean shouldShutDownOnAppExit) {
        return run(commandLineParser.parse(commandLine), maxWaitTime, shouldShutDownOnAppExit);
    }

    public CommandResult runNicely(String commandLine) {
        return run(String.format(S_S, nicelyCommandPrefix, commandLine));
    }

    public CommandResult runNicely(String commandLine, long maxWaitTime) {
        return run(String.format(S_S, nicelyCommandPrefix, commandLine), maxWaitTime);
    }

    public CommandResult runNicelyInShell(String commandLine) {
        return runInShell(String.format(S_S, nicelyCommandPrefix, commandLine));
    }

    public CommandResult runNicelyInShell(String commandLine, long maxWaitTime) {
        return runInShell(String.format(S_S, nicelyCommandPrefix, commandLine), maxWaitTime);
    }

    public CommandResult runInShell(String commandLine) {
        return run(new String[] { BIN_SH, DASH_C, commandLine });
    }

    public CommandResult runInShell(String commandLine, long maxWaitTime) {
        return run(new String[] { BIN_SH, DASH_C, commandLine }, maxWaitTime, DEFAULT_SHOULD_SHUTDOWN_PROCESS_ON_APP_EXIT);
    }

    private CommandResult run(String[] commands) {
        return run(commands, NO_MAX_WAIT_TIME, DEFAULT_SHOULD_SHUTDOWN_PROCESS_ON_APP_EXIT);
    }

    private CommandResult run(String[] commands, long maxWaitTime, boolean shouldShutDownOnAppExit) {
        String commandsAsString = StringUtils.join(commands, ' ');
        LOG.info(String.format("run(%s)", commandsAsString));
        String exceptionMessage = String.format("Error executing %s", commandsAsString);
        try {
            CommandExecutor commandExecutor = createCommandExecutor();
            int rc = commandExecutor.executeScript(commands, Runtime.getRuntime(), LOG_CLI_ERROR_LINES, maxWaitTime, shouldShutDownOnAppExit);
            LOG.info(String.format("rc from '%s' = %d", commandsAsString, rc));
            logOutputLines(commandExecutor.getOutputLines());
            if (rc != 0 && maxWaitTime == NO_MAX_WAIT_TIME) {
                List<String> errorLines = commandExecutor.getErrorLines();
                logErrorLines(errorLines);
                String message = String.format("%s, return code: %d", exceptionMessage, rc);
                throw new CommandExecutionException(message, errorLines);
            }

            return new CommandResult(rc, commandExecutor.getOutputLines(), commandExecutor.getErrorLines());
        } catch (IOException e) {
            LOG.error(exceptionMessage, e);
            throw new CommandExecutionException(exceptionMessage, e);
        } catch (InterruptedException e) {
            LOG.error(exceptionMessage, e);
            throw new CommandExecutionException(exceptionMessage, e);
        }
    }

    protected CommandExecutor createCommandExecutor() {
        return new CommandExecutor(executor);
    }

    private void logOutputLines(List<String> outputLines) {
        if (LOG.isDebugEnabled())
            for (String line : outputLines)
                if (LOG.isDebugEnabled())
                    LOG.debug(line);
    }

    private void logErrorLines(List<String> errorLines) {
        if (LOG.isErrorEnabled())
            for (String line : errorLines)
                if (LOG.isErrorEnabled())
                    LOG.error(line);
    }
}

//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class CommandParser {
    private Map<String, Command> commandMap;
    private Executor executor;
    private List<ManagementCommand> managementCommands;

    public CommandParser() {
        executor = null;
        managementCommands = null;
        commandMap = new HashMap<String, Command>();
    }

    protected Map<String, Command> getCommandMap() {
        return commandMap;
    }

    protected void populateCommandMap() {
        List<Command> commands = new ArrayList<Command>();
        commands.add(new HelpCommand(commandMap));
        commands.add(new ExitCommand());

        for (Command c : commands) {
            commandMap.put(c.getKeyword(), c);
        }

        for (ManagementCommand mc : managementCommands) {
            mc.setExecutor(executor);
            mc.setRuntime(Runtime.getRuntime());
            commandMap.put(mc.getKeyword(), mc);
        }
    }

    protected Executor getExecutor() {
        return executor;
    }

    public Command parse(String inputLine) { // throws CommandParseException
        StringTokenizer stringTokenizer = new StringTokenizer(inputLine);
        String commandKeyword = null;
        List<String> args = new ArrayList<String>();
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (commandKeyword == null)
                commandKeyword = token;
            else
                args.add(token);
        }

        Command command = commandMap.get(commandKeyword);
        if (command == null)
            throw new CommandParseException(String.format("Unknown command: %s", commandKeyword));

        command.setArgs(args.toArray(new String[args.size()]));
        return command;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecService) {
        executor = scheduledExecService;
    }

    @Resource
    public void setExternalManagementCommands(List<ManagementCommand> externalManagementCommands) {
        managementCommands = externalManagementCommands;
        populateCommandMap();
    }
}

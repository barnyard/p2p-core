//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

public class HelpCommand extends KoalaNodeCommand {
	private Map<String, Command> commandMap;

	public HelpCommand(Map<String, Command> aCommandMap) {
		this.commandMap = aCommandMap;
	}

	public void execute(PrintStream outputStream) {
		outputStream.println(String.format("Koala p2p tool"));
		outputStream.println(String.format("--------------"));
		outputStream.println(String.format("Usage: <command> [args] where command is one of:\n"));
		Iterator<String> iter = commandMap.keySet().iterator();
		while (iter.hasNext()) {
			Command c = commandMap.get(iter.next());
			outputStream.println(String.format("   %1$-10s", c.getKeyword()) + " - " + c.getDescription());
		}
		outputStream.println();
	}

	public String getDescription() {
		return "Shows all available commands";
	}

	public String getKeyword() {
		return "help";
	}

}

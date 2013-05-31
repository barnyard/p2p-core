//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.io.PrintStream;

public interface Command {
	String getKeyword();
	String getDescription();
	void execute(PrintStream outputStream); //throws CommandParseException, CommandExecutionException;

	String[] getArgs();
	void setArgs(String[] aArgs);
}

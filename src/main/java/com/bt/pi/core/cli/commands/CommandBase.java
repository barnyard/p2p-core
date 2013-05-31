//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.util.Arrays;

public abstract class CommandBase implements Command {
	private String[] args;

	public CommandBase() {
		args = new String[] {};
	}

	public String[] getArgs() {
		return Arrays.copyOf(args, args.length);
	}

	public void setArgs(String[] aArgs) {
		this.args = Arrays.copyOf(aArgs, aArgs.length);
	}

}

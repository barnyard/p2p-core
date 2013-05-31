/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.core.util.common;

import java.util.List;

public class CommandResult {

	private int returnCode;
	private List<String> outputLines;
	private List<String> errorLines;
	
	public CommandResult(int rc, List<String> oLines, List<String> eLines) {
		this.returnCode = rc;
		this.outputLines = oLines;
		this.errorLines = eLines;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public List<String> getOutputLines() {
		return outputLines;
	}

	public List<String> getErrorLines() {
		return errorLines;
	}

	@Override
	public String toString() {
		return "CommandResult [outputLines=" + outputLines + ", errorLines=" + errorLines + ", returnCode=" + returnCode + "]";
	}
}

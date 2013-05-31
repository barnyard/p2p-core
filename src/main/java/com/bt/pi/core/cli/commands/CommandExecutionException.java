//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandExecutionException extends RuntimeException {
    private static final long serialVersionUID = -4298875396704049847L;
    private final List<String> errorLines;

    public CommandExecutionException() {
        super();
        errorLines = new ArrayList<String>();
    }

    public CommandExecutionException(String message) {
        super(message);
        errorLines = new ArrayList<String>();
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
        errorLines = new ArrayList<String>();
    }

    public CommandExecutionException(String message, List<String> theErrorLines) {
        super(message);
        errorLines = theErrorLines;
    }

    public List<String> getErrorLines() {
        return errorLines;
    }
}
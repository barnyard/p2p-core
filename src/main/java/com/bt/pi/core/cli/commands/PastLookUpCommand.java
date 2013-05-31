package com.bt.pi.core.cli.commands;

import java.util.concurrent.Executor;

public class PastLookUpCommand extends ManagementCommand {

    private static final String LOOKUP = "lookup";

    public PastLookUpCommand() {
        super();
    }

    public PastLookUpCommand(Runtime runtime, Executor executor) {
        super(runtime, executor);
    }

    @Override
    public String getDescription() {
        return "Returns the past data for the given id. Specify the id as the pre-hashed key.";
    }

    @Override
    public String getKeyword() {
        return LOOKUP;
    }

    @Override
    protected String getBeanName() {
        return "storageManagement";
    }

    @Override
    protected String getManagementArgs() {
        return getArgs()[0];
    }

    @Override
    protected String getMethodName() {
        return LOOKUP;
    }
}

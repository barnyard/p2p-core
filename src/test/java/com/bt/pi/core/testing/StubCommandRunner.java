package com.bt.pi.core.testing;

import com.bt.pi.core.util.common.CommandRunner;
import com.ragstorooks.testrr.cli.CommandExecutor;

public class StubCommandRunner extends CommandRunner {
    private CommandExecutor commandExecutor;

    public StubCommandRunner() {
        commandExecutor = null;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    protected CommandExecutor createCommandExecutor() {
        return commandExecutor;
    }
}

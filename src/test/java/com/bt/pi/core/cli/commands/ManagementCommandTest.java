package com.bt.pi.core.cli.commands;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.ragstorooks.testrr.cli.CommandExecutor;

public class ManagementCommandTest {

    private ManagementCommand managementCommand;
    private Runtime runtime;
    private Executor executor;
    private CommandExecutor commandExecutor;

    @Before
    public void before() throws Exception {
        commandExecutor = mock(CommandExecutor.class);

        runtime = mock(Runtime.class);

        executor = mock(Executor.class);

        managementCommand = new ManagementCommand(runtime, executor) {

            @Override
            protected String getBeanName() {
                return "bean";
            }

            @Override
            protected String getManagementArgs() {
                return "hello";
            }

            @Override
            protected String getMethodName() {
                return "methodName";
            }

            @Override
            public String getDescription() {
                return "description";
            }

            @Override
            public String getKeyword() {
                return "keyword";
            }

            @Override
            protected CommandExecutor newCommandExecutor() {
                return commandExecutor;
            }

        };
    }

    @Test
    public void testExecute() throws Exception {
        // setup
        PrintStream printStream = mock(PrintStream.class);

        // act
        managementCommand.execute(printStream);

        // verify
        verify(commandExecutor).executeScript((String[]) anyObject(), eq(runtime));
    }

    @Test
    public void shouldSetAppCtx() {
        // setup

        ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
        Properties properties = new Properties();
        properties.put("TEST_PROP", "TEST_VALUE");
        when(mockApplicationContext.getBean("properties")).thenReturn(properties);
        // act
        managementCommand.setApplicationContext(mockApplicationContext);
    }

    @Test
    public void shouldReturnNewCommandExecutor() {
        assertNotNull(managementCommand.newCommandExecutor());
    }

    @Test
    public void shouldSetExecutor() {
        managementCommand.setExecutor(mock(Executor.class));
    }
}

//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import rice.environment.params.Parameters;

import com.bt.pi.core.environment.KoalaParameters;
import com.ragstorooks.testrr.cli.CommandExecutor;

public abstract class ManagementCommand extends CommandBase implements ApplicationContextAware {
    private static final Log LOG = LogFactory.getLog(ManagementCommand.class);
    private static final String JAVA_COMMAND_PARAM = "java_command";
    private static final String JAR_FILE_PARAM = "jar_file";
    private static final String JMX_CLIENT_OPTIONS_PARAM = "jmx_client_options";
    private static final String USERNAME_PASSWORD_PARAM = "username_password";
    private static final String IP_PORT_PARAM = "ip_port";
    private static final String EQUALS = "=";
    private static final String COMMAND = "%s %s -jar %s %s %s bean:name=%s %s";

    private String javaCommand = "java";
    private String jarFile = "cmdline-jmxclient-0.10.3.jar";
    private String usernamePassword = "-";
    private String jmxClientOptions = "";
    private String ipPort = "localhost:12345";

    private Parameters parameters;

    private Executor executor;
    private Runtime runtime;

    public ManagementCommand() {
    }

    public ManagementCommand(Runtime aRuntime, Executor aExecutor) {
        this.runtime = aRuntime;
        this.executor = aExecutor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        Properties properties = (Properties) applicationContext.getBean("properties");
        setupProps(properties);
    }

    private void setupProps(Properties properties) {
        parameters = new KoalaParameters();
        ((KoalaParameters) parameters).setProperties(properties);
        if (parameters.contains(JAVA_COMMAND_PARAM))
            javaCommand = parameters.getString(JAVA_COMMAND_PARAM);
        if (parameters.contains(JAR_FILE_PARAM))
            jarFile = parameters.getString(JAR_FILE_PARAM);
        if (parameters.contains(JMX_CLIENT_OPTIONS_PARAM))
            jmxClientOptions = parameters.getString(JMX_CLIENT_OPTIONS_PARAM);
        if (parameters.contains(USERNAME_PASSWORD_PARAM))
            usernamePassword = parameters.getString(USERNAME_PASSWORD_PARAM);
        if (parameters.contains(IP_PORT_PARAM))
            ipPort = parameters.getString(IP_PORT_PARAM);
    }

    public void setRuntime(Runtime aRuntime) {
        this.runtime = aRuntime;
    }

    public void setExecutor(Executor aExecutor) {
        this.executor = aExecutor;
    }

    protected abstract String getManagementArgs();

    protected abstract String getMethodName();

    protected abstract String getBeanName();

    @Override
    public void execute(PrintStream outputStream) {
        String method = getMethod(getMethodName(), getManagementArgs());
        String methodResult = String.format("%s: ", method);

        List<String> output = executeManagementCommand(getBeanName(), method);
        if (output != null) {
            for (int i = 0; i < output.size(); i++) {
                String line = output.get(i);
                if (i != 0 || !line.contains(methodResult))
                    outputStream.println(line);
                else {
                    outputStream.println(line.substring(line.indexOf(methodResult) + methodResult.length()));
                }
            }
        }
    }

    protected List<String> executeManagementCommand(String beanName, String method) {
        String command = String.format(COMMAND, javaCommand, jmxClientOptions, jarFile, usernamePassword, ipPort, beanName, method);
        CommandExecutor commandExecutor = newCommandExecutor();
        try {
            commandExecutor.executeScript(command.split(" "), runtime);
        } catch (IOException e) {
            String message = String.format("IOException executing command: %s", command);
            LOG.warn(message, e);
            return Arrays.asList(new String[] { message });
        } catch (InterruptedException e) {
            String message = String.format("InterruptedException executing command: %s", command);
            LOG.warn(message, e);
            return Arrays.asList(new String[] { message });
        }
        return commandExecutor.getErrorLines();
    }

    private String getMethod(String methodName, String args) {
        return String.format("%s%s%s", methodName, args.length() == 0 ? "" : EQUALS, args);
    }

    protected CommandExecutor newCommandExecutor() {
        return new CommandExecutor(executor);
    }
}

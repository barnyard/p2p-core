package com.bt.pi.core.console;

import groovy.util.GroovyMBean;

import java.io.IOException;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServerConnection;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.tools.shell.CommandSupport;
import org.codehaus.groovy.tools.shell.Shell;

public class JmxCommand extends CommandSupport {

    private static final String ERROR_MESSAGE = "Unable to get bean 'bean:name='%s' from JMX";

    public JmxCommand(Shell shell) {
        super(shell, "jmx", "\\j");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object execute(List args) {
        if (args == null || args.size() != 1) {
            fail("Expected exactly one argument");
        }
        String beanName = (String) args.get(0);

        if (StringUtils.isBlank(beanName)) {
            fail("beanName is blank");
        }

        try {
            GroovyMBean mBean = getMBean("bean:name=" + beanName);
            getBinding().setVariable(beanName, mBean);
            return mBean;
        } catch (JMException e) {
            fail(String.format(ERROR_MESSAGE, beanName), e);
            return null;
        } catch (IOException e) {
            fail(String.format(ERROR_MESSAGE, beanName), e);
            return null;
        }
    }

    // for overriding in tests
    GroovyMBean getMBean(String beanName) throws JMException, IOException {
        MBeanServerConnection jmxServer = (MBeanServerConnection) shell.execute("appCtx['exporter'].server");
        return new GroovyMBean(jmxServer, beanName);
    }

    public String getDescription() {
        return "Returns and bind an MBean from JMX to a variable of the same name";
    }

    public String getUsage() {
        return "<jmx_bean_name>";
    }

    public String getHelp() {
        return "Returns and binds a GroovyMBean representation of the JMX bean named 'bean:name=@|BOLD jmx_bean_name|@' to the variable @|BOLD jmx_bean_name|@";
    }
}

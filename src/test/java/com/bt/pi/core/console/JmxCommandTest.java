package com.bt.pi.core.console;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import groovy.util.GroovyMBean;

import java.io.IOException;
import java.util.Arrays;

import javax.management.JMException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.tools.shell.CommandException;
import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;
import org.junit.Before;
import org.junit.Test;

public class JmxCommandTest {

    private IO io;
    private Groovysh groovysh;

    private GroovyMBean mBean;
    private JmxCommand jmxCommand;

    @Before
    public void before() {
        io = mock(IO.class);
        mBean = mock(GroovyMBean.class);

        groovysh = new Groovysh(io);

        jmxCommand = new JmxCommand(groovysh) {
            @Override
            GroovyMBean getMBean(String beanName) {
                return mBean;
            }
        };
    }

    @Test(expected = CommandException.class)
    public void shouldThrowCommandExceptionForNullArguments() {
        jmxCommand.execute(null);
    }

    @Test(expected = CommandException.class)
    public void shouldThrowCommandExceptionForMoreThanOneArgument() {
        jmxCommand.execute(Arrays.asList("a", "b"));
    }

    @Test(expected = CommandException.class)
    public void shouldThrowCommandExceptionForBlankArgument() {
        jmxCommand.execute(Arrays.asList(" "));
    }

    @Test
    public void shouldPrependBeanNameWithQualifierWhenLookingUp() {
        jmxCommand = new JmxCommand(groovysh) {
            @Override
            GroovyMBean getMBean(String beanName) {
                assertEquals("bean:name=foo", beanName);
                return mBean;
            }
        };

        jmxCommand.execute(Arrays.asList("foo"));
    }

    @Test
    public void shouldRegisterBeanNameInBindings() {
        jmxCommand.execute(Arrays.asList("foo"));
        assertEquals(mBean, groovysh.getInterp().getContext().getVariable("foo"));
    }

    @Test
    public void shouldReturnMBean() {
        assertEquals(mBean, jmxCommand.execute(Arrays.asList("foo")));
    }

    @Test(expected = CommandException.class)
    public void shouldThrowCommandExceptionIfJMExceptionIsThrown() {
        jmxCommand = new JmxCommand(new Groovysh(io)) {
            @Override
            GroovyMBean getMBean(String beanName) throws JMException {
                throw new JMException();
            }
        };
        jmxCommand.execute(Arrays.asList("foo"));
    }

    @Test(expected = CommandException.class)
    public void shouldThrowCommandExceptionIfIOExceptionIsThrown() {
        jmxCommand = new JmxCommand(new Groovysh(io)) {
            @Override
            GroovyMBean getMBean(String beanName) throws IOException {
                throw new IOException();
            }
        };
        jmxCommand.execute(Arrays.asList("foo"));
    }

    @Test
    public void shouldGetDescription() {
        assertFalse(StringUtils.isEmpty(jmxCommand.getDescription()));
    }

    @Test
    public void shouldGetHelp() {
        assertFalse(StringUtils.isEmpty(jmxCommand.getHelp()));
    }

    @Test
    public void shouldGetUsage() {
        assertFalse(StringUtils.isEmpty(jmxCommand.getUsage()));
    }

}

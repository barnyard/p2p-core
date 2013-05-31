package com.bt.pi.core.cli.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.commands.PastLookUpCommand;

public class PastLookUpCommandTest {

    private static final String LOOKUP = "lookup";
    private PastLookUpCommand pastLookUpCommand;

    @Before
    public void before() {
        pastLookUpCommand = new PastLookUpCommand();
    }

    @Test
    public void testGetKeyword() {
        assertEquals(LOOKUP, pastLookUpCommand.getKeyword());
    }

    @Test
    public void testGetDescription() {
        assertTrue(pastLookUpCommand.getDescription().contains("past data"));
    }

    @Test
    public void testGetMethodName() {
        assertEquals(LOOKUP, pastLookUpCommand.getMethodName());
    }

    @Test
    public void testGetBeanName() {
        assertEquals("storageManagement", pastLookUpCommand.getBeanName());
    }
}

package com.bt.pi.core.cli.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.commands.Command;
import com.bt.pi.core.cli.commands.HelpCommand;


public class HelpCommandTest  {
	private HelpCommand helpCommand;
	private Map<String, Command> commandMap;
	private PrintStream printStream;
	private ByteArrayOutputStream byteArrayOutputStream;
	
	@Before
	public void before() {
		byteArrayOutputStream = new ByteArrayOutputStream();
		printStream = new PrintStream(byteArrayOutputStream);
		
		commandMap = new HashMap<String, Command>();
		helpCommand = new HelpCommand(commandMap);
		
		commandMap.put(helpCommand.getKeyword(), helpCommand);
	}

	@Test
	public void shouldHaveDescription() {
		// act
		String desc = helpCommand.getDescription();
		
		// assert
		assertNotNull(desc);
	}
	
	@Test
	public void shouldHaveKeyword() {
		// act
		String keyword = helpCommand.getKeyword();
		
		// assert
		assertEquals("help", keyword);
	}
	
	@Test
	public void shouldPrintHelpMessage() {
		// act
		helpCommand.execute(printStream);
		
		// assert
		String out = new String(byteArrayOutputStream.toByteArray());
		System.err.println(out);		
		assertTrue(out.trim().indexOf("help") > -1);
		assertTrue(out.indexOf(helpCommand.getDescription()) > -1);
	}
}

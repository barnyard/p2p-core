package com.bt.pi.core.cli.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.commands.ExitCommand;

public class ExitCommandTest {
	private ExitCommand exitCommand;

	@Before
	public void before() {
		this.exitCommand = new ExitCommand();
	}

	@Test
	public void shouldPrintStuff() {
		PrintStream out = mock(PrintStream.class);

		// act
		exitCommand.execute(out);

		// verify
		verify(out, times(9)).println(isA(String.class));
	}

	@Test
	public void shouldHaveDescription() {
		// act
		String desc = exitCommand.getDescription();

		// assert
		assertNotNull(desc);
	}

	@Test
	public void shouldHaveKeyword() {
		// act
		String keyword = exitCommand.getKeyword();

		// assert
		assertEquals("exit", keyword);
	}
}

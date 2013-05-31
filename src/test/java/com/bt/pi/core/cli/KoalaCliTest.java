package com.bt.pi.core.cli;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.cli.KoalaCli;
import com.bt.pi.core.cli.commands.Command;
import com.bt.pi.core.cli.commands.CommandExecutionException;
import com.bt.pi.core.cli.commands.CommandParseException;
import com.bt.pi.core.cli.commands.CommandParser;
import com.bt.pi.core.cli.commands.KoalaNodeCommand;
import com.bt.pi.core.node.KoalaNode;

public class KoalaCliTest {
	private KoalaCli koalaCli;
	private InputStream testStream;
	private CommandParser commandParser;
	private KoalaNode koalaNode;
	private Command goodCommand;
	private Command badCommand;
	private KoalaNodeCommand koalaNodeCommand;

	@Before
	public void before() {
		koalaNode = mock(KoalaNode.class);

		goodCommand = mock(Command.class);
		koalaNodeCommand = mock(KoalaNodeCommand.class);
		badCommand = mock(Command.class);
		doThrow(new CommandExecutionException("oops - exec")).when(badCommand).execute(isA(PrintStream.class));

		commandParser = mock(CommandParser.class);
		when(commandParser.parse("hello")).thenReturn(goodCommand);
		when(commandParser.parse("failparse")).thenThrow(new CommandParseException("oops - parse"));
		when(commandParser.parse("failexec")).thenReturn(badCommand);
		when(commandParser.parse("koala")).thenReturn(koalaNodeCommand);

		koalaCli = new KoalaCli(koalaNode) {
			@Override
			protected InputStream getInputStream() {
				return testStream;
			}
		};

		koalaCli.setCommandParser(commandParser);
	}

	@After
	public void after() {
		if (koalaCli != null)
			koalaCli.stop();
	}

	/**
	 * Should delegate command processing to command parser
	 */
	@Test
	public void shouldDelegateCommandProcessingToCommandParser() throws Exception {
		// setup
		testStream = new ByteArrayInputStream(new String("hello\n").getBytes());

		// act
		koalaCli.start();
		while (testStream.available() > 0)
			Thread.sleep(100);

		// assert
		verify(commandParser).parse("hello");
		verify(goodCommand).execute(isA(PrintStream.class));
	}

	/**
	 * Should absorb parse exception
	 */
	@Test
	public void shouldAbsorbParseException() throws Exception {
		// setup
		testStream = new ByteArrayInputStream(new String("failparse\n").getBytes());

		// act
		koalaCli.start();
		while (testStream.available() > 0)
			Thread.sleep(100);

		// assert
		// no exception
	}

	/**
	 * Should absorb exec exception
	 */
	@Test
	public void shouldAbsorbExecException() throws Exception {
		// setup
		testStream = new ByteArrayInputStream(new String("failexec\n").getBytes());

		// act
		koalaCli.start();
		while (testStream.available() > 0)
			Thread.sleep(100);

		// assert
		// no exception
	}

	/**
	 * Should populate koala node on koala command
	 */
	@Test
	public void shouldSetKoalaNodeOnKoalaCommand() throws Exception {
		// setup
		testStream = new ByteArrayInputStream(new String("koala\n").getBytes());

		// act
		koalaCli.start();
		while (testStream.available() > 0)
			Thread.sleep(100);

		// assert
		verify(koalaNodeCommand).setKoalaNode(koalaNode);
		verify(koalaNodeCommand).execute(isA(PrintStream.class));
	}

}

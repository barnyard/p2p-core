package com.bt.pi.core.cli.commands;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.bt.pi.core.cli.commands.KoalaNodeCommand;
import com.bt.pi.core.node.KoalaNode;

public class KoalaNodeCommandTest {
	private KoalaNodeCommand koalaNodeCommand;
	private KoalaNode koalaNode;

	@Before
	public void before() {
		koalaNode = Mockito.mock(KoalaNode.class);

		this.koalaNodeCommand = new KoalaNodeCommand() {
			public void execute(PrintStream outputStream) {
			}

			public String getDescription() {
				return "desc";
			}

			public String getKeyword() {
				return "test";
			}
		};
	}

	@Test
	public void testGetterAndSetter() {
		// act
		koalaNodeCommand.setKoalaNode(koalaNode);

		// assert
		assertEquals(koalaNode, koalaNodeCommand.getKoalaNode());
	}
}
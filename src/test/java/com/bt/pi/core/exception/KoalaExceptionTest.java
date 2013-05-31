package com.bt.pi.core.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.core.exception.KoalaException;

public class KoalaExceptionTest {

	@Test
	public void constructorTest() {
		String pooh = "pooh";
		KoalaException koalaException = new KoalaException(pooh);

		assertTrue(koalaException.getMessage().contains(pooh));
	}
}

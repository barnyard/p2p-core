package com.bt.pi.core.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.core.exception.KoalaContentVersionMismatchException;

public class KoalaContentVersionMismatchExceptionTest {

	@Test
	public void constructorTest() {
		String pooh = "pooh";
		KoalaContentVersionMismatchException koalaException = new KoalaContentVersionMismatchException(pooh);

		assertTrue(koalaException.getMessage().contains(pooh));
	}
}

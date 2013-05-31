package com.bt.pi.core.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.core.exception.KoalaMessageTimeoutException;

public class KoalaMessageTimeoutExceptionTest {

	@Test
	public void contructorTest() {
		KoalaMessageTimeoutException exeption = new KoalaMessageTimeoutException("kitkat");

		assertTrue(exeption.getMessage().contains("kitkat"));
	}
}

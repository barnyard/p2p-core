package com.bt.pi.core.exception;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.exception.KoalaNodeInitializationException;

public class KoalaNodeInitializationExceptionTest {

	private String message = "inABottle";
	private Throwable t;

	@Before
	public void before() {
		t = mock(Throwable.class);
	}

	@Test
	public void testStringContructor() {
		KoalaNodeInitializationException koalaNodeInitializationException = new KoalaNodeInitializationException(
				message);

		assertEquals(message, koalaNodeInitializationException.getMessage());
	}

	@Test
	public void testThrowableConstructor() {
		KoalaNodeInitializationException koalaNodeInitializationException = new KoalaNodeInitializationException(
				message, t);

		assertEquals(message, koalaNodeInitializationException.getMessage());
		assertEquals(t, koalaNodeInitializationException.getCause());
	}
}

package com.bt.pi.core.exception;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.exception.KoalaScenarioException;

public class KoalaScenarioExceptionTest {
	private String message = "inABottle";
	private Throwable t;

	@Before
	public void before() {
		t = mock(Throwable.class);
	}

	@Test
	public void testStringContructor() {
		KoalaScenarioException exception = new KoalaScenarioException(message);

		assertEquals(message, exception.getMessage());
	}

	@Test
	public void testThrowableConstructor() {
		KoalaScenarioException exception = new KoalaScenarioException(message, t);

		assertEquals(message, exception.getMessage());
		assertEquals(t, exception.getCause());
	}
}

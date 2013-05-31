package com.bt.pi.core.exception;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.exception.KoalaScribeException;

public class KoalaScribeExceptionTest {
    private String message = "inABottle";
    private Throwable t;

    @Before
    public void before() {
        t = mock(Throwable.class);
    }

    @Test
    public void testStringContructor() {
        KoalaScribeException exception = new KoalaScribeException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testStringThrowableConstructor() {
        KoalaScribeException exception = new KoalaScribeException(message, t);

        assertEquals(message, exception.getMessage());
        assertEquals(t, exception.getCause());
    }

    @Test
    public void testThrowableConstructor() {
        Exception foo = new Exception("foo");
        KoalaScribeException exception = new KoalaScribeException(foo);

        assertEquals(foo, exception.getCause());
    }
}

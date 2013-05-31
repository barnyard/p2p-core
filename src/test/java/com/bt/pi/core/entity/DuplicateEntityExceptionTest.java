package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DuplicateEntityExceptionTest {
    private DuplicateEntityException exception;

    @Test
    public void testConstructor() {
        exception = new DuplicateEntityException("hello");

        assertEquals("hello", exception.getMessage());
    }

}

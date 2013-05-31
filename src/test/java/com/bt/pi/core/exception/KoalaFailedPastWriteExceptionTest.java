package com.bt.pi.core.exception;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bt.pi.core.exception.PiInsufficientResultsException;

public class KoalaFailedPastWriteExceptionTest {

    @Test
    public void contructorTest() {
        PiInsufficientResultsException exeption = new PiInsufficientResultsException("kitkat");

        assertTrue(exeption.getMessage().contains("kitkat"));
    }

}

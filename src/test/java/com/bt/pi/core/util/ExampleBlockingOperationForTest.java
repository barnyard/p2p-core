package com.bt.pi.core.util;

import org.junit.Test;
import org.springframework.stereotype.Component;

@Component
public class ExampleBlockingOperationForTest {

    public ExampleBlockingOperationForTest() {

    }

    @Test
    public void dummyTest() {

    }

    @Blocking
    public void blockingMethod() {
        System.err.println("This line should not be printed and an exception thrown instead");
    }
}

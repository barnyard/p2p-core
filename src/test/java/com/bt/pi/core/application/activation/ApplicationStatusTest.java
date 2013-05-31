package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.core.application.activation.ApplicationStatus;

public class ApplicationStatusTest {

    @Test
    public void testGet() {
        assertEquals(ApplicationStatus.ACTIVE, ApplicationStatus.get(ApplicationStatus.ACTIVE.getStatus()));
    }
}

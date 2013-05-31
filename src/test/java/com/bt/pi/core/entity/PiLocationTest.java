package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.scope.NodeScope;

public class PiLocationTest {
    private PiLocation piLocation;

    @Before
    public void before() {
        piLocation = new PiLocation("abc:def", NodeScope.REGION);
    }

    @Test
    public void testToString() {
        assertTrue(piLocation.toString().contains("abc:def"));
    }

    @Test
    public void testEquals() {
        assertTrue(piLocation.equals(new PiLocation("abc:def", NodeScope.REGION)));
        assertFalse(piLocation.equals(new PiLocation("abc:ddd", NodeScope.REGION)));
        assertFalse(piLocation.equals(new PiLocation("abc:def", NodeScope.AVAILABILITY_ZONE)));
    }

    @Test
    public void testHashcode() {
        assertEquals(piLocation.hashCode(), new PiLocation("abc:def", NodeScope.REGION).hashCode());
    }
}

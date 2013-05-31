package com.bt.pi.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MutableStringTest {
    @Test
    public void shouldInitializeToNullByDefault() {
        assertNull(new MutableString().get());
    }

    @Test
    public void shouldBeAbleToInitToGivenString() {
        assertEquals("aaa", new MutableString("aaa").get());
    }

    @Test
    public void shouldBeAbleToSetToDifferentValue() {
        // setup
        MutableString mutableString = new MutableString("aaa");

        // act
        mutableString.set("bbb");

        // assert
        assertEquals("bbb", mutableString.get());
    }

    @Test
    public void testToString() {
        assertEquals("ccc", new MutableString("ccc").toString());
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(new MutableString("ccc").equals(null));
    }

    @Test
    public void testEqualsWithRandomObject() {
        assertFalse(new MutableString("ccc").equals(new Object()));
    }

    @Test
    public void testEqualsWithDifferentString() {
        assertFalse(new MutableString("ccc").equals("ddd"));
    }

    @Test
    public void testEqualsWithDifferentMutableString() {
        assertFalse(new MutableString("ccc").equals(new MutableString("ddd")));
    }

    @Test
    public void testEqualsWithSameString() {
        assertTrue(new MutableString("ccc").equals("ccc"));
    }

    @Test
    public void testEqualsWithSameMutableString() {
        assertTrue(new MutableString("ccc").equals(new MutableString("ccc")));
    }

    @Test
    public void testHashcodeWithDifferentMutableStrings() {
        assertFalse(new MutableString("ccc").hashCode() == new MutableString("ddd").hashCode());
    }

    @Test
    public void testHashcodeWithSameMutableStrings() {
        assertTrue(new MutableString("ccc").hashCode() == new MutableString("ccc").hashCode());
    }
}

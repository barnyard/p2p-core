package com.bt.pi.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PiUriParserTest {
    private PiUriParser piUriParser;

    @Test
    public void testSimpleUri() {
        piUriParser = new PiUriParser("inst:i-1234");

        // assert
        assertEquals("inst", piUriParser.getScheme());
        assertEquals("i-1234", piUriParser.getResourceId());
        assertEquals(0, piUriParser.getParameterMap().size());
    }

    @Test
    public void testSimpleUriWithSingleParam() {
        piUriParser = new PiUriParser("inst:i-1234;a=b");

        // assert
        assertEquals("inst", piUriParser.getScheme());
        assertEquals("i-1234", piUriParser.getResourceId());
        assertEquals(1, piUriParser.getParameterMap().size());
        assertEquals("b", piUriParser.getParameterMap().get("a"));
    }

    @Test
    public void testSimpleUriWithMultipleParams() {
        piUriParser = new PiUriParser("inst:i-1234;a=b;cc=123;d=");

        // assert
        assertEquals("inst", piUriParser.getScheme());
        assertEquals("i-1234", piUriParser.getResourceId());
        assertEquals(3, piUriParser.getParameterMap().size());
        assertEquals("b", piUriParser.getParameterMap().get("a"));
        assertEquals("123", piUriParser.getParameterMap().get("cc"));
        assertEquals("", piUriParser.getParameterMap().get("d"));
    }

    @Test
    public void testSimpleUriWithParamsWithUnusualChars() {
        piUriParser = new PiUriParser("inst:i-1234;ab=cd:ef;i-j=k!l");

        // assert
        assertEquals("inst", piUriParser.getScheme());
        assertEquals("i-1234", piUriParser.getResourceId());
        assertEquals(2, piUriParser.getParameterMap().size());
        assertEquals("cd:ef", piUriParser.getParameterMap().get("ab"));
        assertEquals("k!l", piUriParser.getParameterMap().get("i-j"));
    }

    @Test(expected = PiUriParseException.class)
    public void shouldThrowWhenMissingScheme() {
        piUriParser = new PiUriParser(":i-1234;a=b");
    }

    @Test(expected = PiUriParseException.class)
    public void shouldThrowWhenNoSchemeSeparator() {
        piUriParser = new PiUriParser("i-1234;a=b");
    }

    @Test(expected = PiUriParseException.class)
    public void shouldThrowWhenBadParam() {
        piUriParser = new PiUriParser("inst:i-1234;ab");
    }

    @Test(expected = PiUriParseException.class)
    public void shouldThrowWhenMultipleEqualsInParam() {
        piUriParser = new PiUriParser("inst:i-1234;a=b=c");
    }
}

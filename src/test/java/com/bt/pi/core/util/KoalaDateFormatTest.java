package com.bt.pi.core.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

public class KoalaDateFormatTest {

    private KoalaDateFormat koalaDateFormat;

    @Before
    public void before() {
        koalaDateFormat = new KoalaDateFormat();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testFormat() {
        long time = 334404000000L;
        Date date = new Date(time);

        assertEquals("1980.08.06", koalaDateFormat.format(date));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testFormat2() {
        Date date = new Date(109, 9, 14, 0, 0, 0);

        assertEquals("2009.10.14", koalaDateFormat.format(date));
    }

    @Test
    public void testUtcSet() {
        assertEquals(TimeZone.getTimeZone("UTC"), koalaDateFormat.getTimeZone());
    }

}

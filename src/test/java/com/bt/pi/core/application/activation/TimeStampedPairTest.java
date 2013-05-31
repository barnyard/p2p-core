package com.bt.pi.core.application.activation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class TimeStampedPairTest {

    private TimeStampedPair<String> timeStampedPair;

    @Before
    public void before() {
        timeStampedPair = new TimeStampedPair<String>("hello", 1L);
    }

    @Test
    public void testGettersAndSetters() {
        // act
        timeStampedPair.setTimeStamp(3L);
        timeStampedPair.setObject("bob");

        assertEquals(3L, timeStampedPair.getTimeStamp());
        assertEquals(new Date(3L).toString(), timeStampedPair.getReadableTimestamp());
        assertEquals("bob", timeStampedPair.getObject());
    }

    @Test
    public void testToString() {
        assertTrue(timeStampedPair.toString().indexOf("hello") > -1);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowNPEForNullItem() {
        timeStampedPair = new TimeStampedPair<String>(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowNPEForNullItemWithTs() {
        timeStampedPair = new TimeStampedPair<String>(null, 1234L);
    }

    @Test
    public void testThatHostNameIsSet() throws Exception {
        // act
        String s = timeStampedPair.toString();
        String hostname = s.substring(s.lastIndexOf(',') + 1, s.length() - 1);

        // assert
        assertEquals(hostname, InetAddress.getLocalHost().getHostName());
    }
}

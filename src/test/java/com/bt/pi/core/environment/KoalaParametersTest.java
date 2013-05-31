package com.bt.pi.core.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rice.environment.params.ParameterChangeListener;

public class KoalaParametersTest {
    private KoalaParameters koalaParameters;
    private String configFileName = getClass().getSimpleName() + ".properties";

    @Before
    public void setUp() throws Exception {
        koalaParameters = new KoalaParameters();
        Properties p = new Properties();
        p.load(new FileInputStream(new File("src/test/resources/" + configFileName)));
        koalaParameters.setProperties(p);
    }

    @After
    public void after() throws Exception {
    }

    @Test
    public void testAddChangeListener() {
        // setup
        ParameterChangeListener changeListener = mock(ParameterChangeListener.class);

        // act
        this.koalaParameters.addChangeListener(changeListener);

        // assert
        assertEquals(1, this.koalaParameters.getChangeListeners().size());
    }

    @Test
    public void testContains() {
        // setup
        String key = "key";

        // act
        boolean result = koalaParameters.contains(key);

        // assert
        assertTrue(result);
    }

    @Test
    public void testGetBoolean() {
        // setup
        String key = "booleankey";

        // act
        boolean result = koalaParameters.getBoolean(key);

        // assert
        assertTrue(result);
    }

    @Test
    public void testGetDouble() {
        // setup
        String key = "doublekey";

        // act
        double result = koalaParameters.getDouble(key);

        // assert
        assertEquals(12.34, result, 0);
    }

    @Test
    public void testGetFloat() {
        // setup
        String key = "doublekey";

        // act
        float result = koalaParameters.getFloat(key);

        // assert
        assertEquals(12.34, result, .001);
    }

    @Test
    public void testGetInetAddress() throws UnknownHostException {
        // setup
        String key = "google";

        // act
        InetAddress result = koalaParameters.getInetAddress(key);

        // assert
        assertEquals("google.com", result.getHostName());
    }

    @Test
    public void testTrimAllWhitespacesFromPreferredBootstrapItems() throws UnknownHostException {
        // setup
        String key = "arrayOfAddressesWithSpaces";

        // act
        InetSocketAddress[] result = koalaParameters.getInetSocketAddressArray(key);

        // assert
        assertEquals(2, result.length);
        assertEquals("google.com", result[0].getHostName());
        assertEquals(8080, result[0].getPort());
        assertEquals("localhost", result[1].getHostName());
        assertEquals(7070, result[1].getPort());
    }

    @Test
    public void testGetInetSocketAddress() throws UnknownHostException {
        // setup
        String key = "localhost";

        // act
        InetSocketAddress result = koalaParameters.getInetSocketAddress(key);

        // assert
        assertEquals("google.com", result.getHostName());
        assertEquals(8080, result.getPort());
    }

    @Test
    public void testGetInetSocketAddressArray() throws UnknownHostException {
        // setup
        String key = "arrayOfAddresses";

        // act
        InetSocketAddress[] result = koalaParameters.getInetSocketAddressArray(key);

        // assert
        assertEquals(2, result.length);
        assertEquals("google.com", result[0].getHostName());
        assertEquals(8080, result[0].getPort());
        assertEquals("localhost", result[1].getHostName());
        assertEquals(7070, result[1].getPort());
    }

    @Test
    public void testGetInt() {
        // setup
        String key = "int";

        // act
        int result = koalaParameters.getInt(key);

        // assert
        assertEquals(2, result);
    }

    @Test
    public void testGetLong() {
        // setup
        String key = "int";

        // act
        long result = koalaParameters.getLong(key);

        // assert
        assertEquals(2, result);
    }

    @Test
    public void testGetString() {
        // setup
        String key = "int";

        // act
        String result = koalaParameters.getString(key);

        // assert
        assertEquals("2", result);
    }

    @Test
    public void testGetStringArray() {
        // setup
        String key = "arrayOfAddresses";

        // act
        String[] result = koalaParameters.getStringArray(key);

        // assert
        assertEquals(2, result.length);
        assertEquals("google.com:8080", result[0]);
        assertEquals("localhost:7070", result[1]);
    }

    @Test
    public void testRemove() {
        // setup
        String key = "int";
        assertEquals("2", koalaParameters.getString(key));

        // act
        koalaParameters.remove(key);

        // assert
        assertNull(this.koalaParameters.getString(key));
    }

    @Test
    public void testRemoveChangeListener() {
        // setup
        ParameterChangeListener changeListener = mock(ParameterChangeListener.class);
        this.koalaParameters.addChangeListener(changeListener);
        assertEquals(1, this.koalaParameters.getChangeListeners().size());

        // act
        this.koalaParameters.removeChangeListener(changeListener);

        // assert
        assertEquals(0, this.koalaParameters.getChangeListeners().size());
    }

    @Test(expected = NotImplementedException.class)
    public void testSetBoolean() {
        this.koalaParameters.setBoolean("v", true);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetDouble() {
        this.koalaParameters.setDouble("v", 12.23);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetFloat() {
        this.koalaParameters.setFloat("v", (float) 12.123);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetInetAddress() {
        this.koalaParameters.setInetAddress("v", null);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetInetSocketAddress() {
        this.koalaParameters.setInetSocketAddress("v", null);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetInetSocketAddressArray() {
        this.koalaParameters.setInetSocketAddressArray("v", new InetSocketAddress[] {});
    }

    @Test(expected = NotImplementedException.class)
    public void testSetInt() {
        this.koalaParameters.setInt("v", 123);
    }

    @Test(expected = NotImplementedException.class)
    public void testSetLong() {
        this.koalaParameters.setLong("v", 123);
    }

    @Test
    public void testSetString() {
        // setup
        assertEquals("2", this.koalaParameters.getString("int"));

        // act
        this.koalaParameters.setString("int", "3");

        // assert
        assertEquals("3", this.koalaParameters.getString("int"));
    }

    @Test
    public void testSetStringArray() {
        // act
        this.koalaParameters.setStringArray("v", new String[] { "1", "2" });

        // assert
        assertEquals("1,2", this.koalaParameters.getString("v"));
    }

    @Test
    public void testStore() throws IOException {
        this.koalaParameters.store(); // no-op
    }
}

package com.bt.pi.core.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.bt.pi.core.exception.KoalaNodeInitializationException;
import com.bt.pi.core.node.inet.KoalaNodeInetAddressFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KoalaNodeInetAddressFactoryTest {

    @Resource
    KoalaNodeInetAddressFactory koalaNodeInetAddressFactory;

    @Test
    public void shouldGetIpAddress() throws Exception {
        // act
        InetAddress inetAddress = koalaNodeInetAddressFactory.lookupInetAddress();

        // assert
        assertNotNull(inetAddress);
    }

    @Test(expected = KoalaNodeInitializationException.class)
    public void shouldGetExceptionIfNoIpAddresses() throws Exception {
        // setup
        final Vector<NetworkInterface> networkInterfaces = new Vector<NetworkInterface>();
        KoalaNodeInetAddressFactory aKoalaNodeInetAddressFactory = new KoalaNodeInetAddressFactory() {

            @Override
            protected Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
                return networkInterfaces.elements();
            }
        };
        copyTo(aKoalaNodeInetAddressFactory);

        // act
        aKoalaNodeInetAddressFactory.lookupInetAddress();
    }

    @Test(expected = KoalaNodeInitializationException.class)
    public void shouldWrapSocketExceptionWhenNetworkInterfaces() throws Exception {
        // setup
        KoalaNodeInetAddressFactory aKoalaNodeInetAddressFactory = new KoalaNodeInetAddressFactory() {

            @Override
            protected Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
                throw new SocketException("oops");
            }
        };
        copyTo(aKoalaNodeInetAddressFactory);
        // act
        aKoalaNodeInetAddressFactory.lookupInetAddress();
    }

    @Test
    public void shouldCheckPreferredAddresses() throws Exception {
        // setup
        final AtomicInteger count = new AtomicInteger(0);
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        KoalaNodeInetAddressFactory aKoalaNodeInetAddressFactory = new KoalaNodeInetAddressFactory() {

            @Override
            protected NetworkInterface getNetworkInterface(String name) throws SocketException {
                if (count.get() == 0) {
                    assertEquals("test", name);
                    count.incrementAndGet();
                } else if (count.get() == 1) {
                    assertEquals("test2", name);
                    count.incrementAndGet();
                } else
                    fail("Shouldn't have more than 2 interfaces");
                return null;
            }

            @Override
            protected Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
                return networkInterfaces;
            }
        };
        copyTo(aKoalaNodeInetAddressFactory);
        aKoalaNodeInetAddressFactory.setAddressPattern("^127.*");
        aKoalaNodeInetAddressFactory.setPreferredNetworkInterfaceList("test,test2");

        // act
        InetAddress inetAddress = aKoalaNodeInetAddressFactory.lookupInetAddress();

        // assert
        assertEquals(2, count.get());
        assertEquals("/127.0.0.1", inetAddress.toString());
    }

    @Test(expected = KoalaNodeInitializationException.class)
    public void shouldBarfIfNoMatchingAddressPatternToBindTo() throws Exception {
        // setup
        koalaNodeInetAddressFactory.setAddressPattern("^nomatch");

        // act
        koalaNodeInetAddressFactory.lookupInetAddress();
    }

    @Test
    public void shouldBeAbleToSpecifyHostnamePatternToBindTo() throws Exception {
        // setup
        if (System.getProperty("os.name").toLowerCase().contains("window")) {
            return;
        } else {
            koalaNodeInetAddressFactory.setAddressPattern("^lo.*");
        }

        // act
        InetAddress inetAddress = koalaNodeInetAddressFactory.lookupInetAddress();

        // assert
        if (inetAddress instanceof Inet4Address) {
            assertTrue(inetAddress.toString().contains("127.0.0.1"));
        } else if (inetAddress instanceof Inet6Address) {
            assertTrue(inetAddress.toString().contains("0:0:0:0:0:0:0"));
        }
    }

    @Test
    public void shouldBeAbleToSpecifyAddressPatternToBindTo() throws Exception {
        // setup
        koalaNodeInetAddressFactory.setAddressPattern("^127.*");

        // act
        InetAddress inetAddress = koalaNodeInetAddressFactory.lookupInetAddress();

        // assert
        assertEquals("/127.0.0.1", inetAddress.toString());
    }

    private void copyTo(KoalaNodeInetAddressFactory aKoalaNodeInetAddressFactory) {
        copyField(aKoalaNodeInetAddressFactory, "parameters");
    }

    private void copyField(KoalaNodeInetAddressFactory aKoalaNodeInetAddressFactory, String field) {
        ReflectionTestUtils.setField(aKoalaNodeInetAddressFactory, field, ReflectionTestUtils.getField(koalaNodeInetAddressFactory, field));

    }

}

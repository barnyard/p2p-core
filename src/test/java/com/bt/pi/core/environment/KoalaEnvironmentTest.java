package com.bt.pi.core.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import rice.environment.params.Parameters;

import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.pastry_override.PiSelectorManager;
import com.bt.pi.core.pastry_override.PiSelectorManager;

public class KoalaEnvironmentTest {

    KoalaEnvironment environment;

    @Before
    public void before() throws IOException {
        environment = new KoalaEnvironment();
        environment.setLogManager(new Log4JLogManager());
        ReflectionTestUtils.setField(environment, "selectorManager", new PiSelectorManager());
        KoalaParameters params = new KoalaParameters();
        Properties p = new Properties();
        p.load(getClass().getClassLoader().getResourceAsStream("p2p.properties"));
        params.setProperties(p);
        environment.setParameters(params);
    }

    @Test
    public void testGettersAndSetters() {
        // setup
        Parameters params = mock(Parameters.class);

        // act
        environment.setParameters(params);

        // assert
        assertEquals(params, environment.getParameters());
    }

    @Test
    public void initShouldCreatePastryEnvironment() throws UnknownHostException {

        // setup

        PiSelectorManager selectorManager = mock(PiSelectorManager.class);
        ReflectionTestUtils.setField(environment, "selectorManager", selectorManager);
        File log4jMockFile = mock(File.class);
        when(log4jMockFile.getAbsolutePath()).thenReturn("src/test/resources/log4j.xml");

        environment.setLog4jFile(log4jMockFile);

        // act
        environment.initPastryEnvironment(InetAddress.getLocalHost(), 1234);

        // assert
        assertNotNull(environment.getPastryEnvironment());
    }
}
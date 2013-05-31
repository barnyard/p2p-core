/**
 * 
 */
package com.bt.pi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.apache.commons.cli.CommandLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.bt.pi.core.node.KoalaNode;

public class CloudPlatformTest {

    private KoalaNode koalaNode;
    private CommandLine commandLine;
    private CloudPlatform cloudPlatform;

    @Before
    public void setUp() throws Exception {
        koalaNode = mock(KoalaNode.class);
        commandLine = mock(CommandLine.class);

        cloudPlatform = new CloudPlatform();
        cloudPlatform.setKoalaNode(koalaNode);
    }

    @Test
    public void shouldStartKoalaNode() {
        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(koalaNode).start();
    }

    @Test
    public void shouldStopKoalaNode() {
        // act
        cloudPlatform.stop();

        // assert
        verify(koalaNode).stop();
    }

    @Test
    public void shouldHaveShutdownHookHandlingMethod() throws Exception {
        // setup
        Method method = CloudPlatform.class.getMethod("stop");

        // assert
        assertTrue(method.getAnnotations()[0] instanceof javax.annotation.PreDestroy);
    }

    @Test
    public void shouldLoadAddressPathIfSetOnCommandLine() {
        // setup
        String addressPattern = "addressPattern";

        when(commandLine.hasOption("a")).thenReturn(true);
        when(commandLine.getOptionValue("a")).thenReturn(addressPattern);

        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(commandLine).getOptionValue("a");
        verify(koalaNode).setAddressPattern(addressPattern);
    }

    @Test
    public void shouldSetPortIfSetOnCommandLine() {
        // setup
        String port = "50";

        when(commandLine.hasOption("p")).thenReturn(true);
        when(commandLine.getOptionValue("p")).thenReturn(port);

        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(commandLine).getOptionValue("p");
        verify(koalaNode).setPort(50);
    }

    @Test
    public void shouldSetPreferredBootstrapIfSetOnCommandLine() {
        // setup
        String prefferedBootstrap = "abc";

        when(commandLine.hasOption("b")).thenReturn(true);
        when(commandLine.getOptionValue("b")).thenReturn(prefferedBootstrap);

        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(commandLine).getOptionValue("b");
        verify(koalaNode).setPreferredBootstraps(prefferedBootstrap);
    }

    @Test
    public void shouldSetNodeIdFileWhenInSingleRunMode() {
        // setup
        ArgumentCaptor<String> nodeIdFile = ArgumentCaptor.forClass(String.class);
        String port = "50";
        String addressPattern = "addressPattern";

        when(commandLine.hasOption("a")).thenReturn(true);
        when(commandLine.getOptionValue("a")).thenReturn(addressPattern);
        when(commandLine.hasOption("p")).thenReturn(true);
        when(commandLine.getOptionValue("p")).thenReturn(port);
        when(commandLine.hasOption("s")).thenReturn(true);

        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(koalaNode).setNodeIdFile(nodeIdFile.capture());
        assertTrue(nodeIdFile.getValue().contains(port));
        assertTrue(nodeIdFile.getValue().contains(addressPattern));
    }

    @Test
    public void shouldSetNodeIdFileWhenInSingleRunModeAndPortAndAddressAreNull() {
        // setup
        ArgumentCaptor<String> nodeIdFile = ArgumentCaptor.forClass(String.class);
        when(commandLine.hasOption("a")).thenReturn(false);
        when(commandLine.getOptionValue("a")).thenReturn(null);
        when(commandLine.hasOption("p")).thenReturn(false);
        when(commandLine.getOptionValue("p")).thenReturn(null);
        when(commandLine.hasOption("s")).thenReturn(true);

        // act
        cloudPlatform.start(commandLine);

        // assert
        verify(koalaNode).setNodeIdFile(nodeIdFile.capture());
        assertTrue(nodeIdFile.getValue().contains("nodeId"));
    }

    @Test
    public void shouldRethrowThrowable() {
        // setup
        RuntimeException ex = mock(RuntimeException.class);
        doThrow(ex).when(koalaNode).start();

        // act
        try {
            cloudPlatform.start(commandLine);
            fail();
        } catch (RuntimeException t) {
            assertEquals(ex, t.getCause());
        }
    }
}

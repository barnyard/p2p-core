package com.bt.pi.core.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import rice.environment.Environment;
import rice.pastry.PastryNode;

import com.bt.pi.core.application.KoalaPastryApplicationBase;
import com.bt.pi.core.application.ReceivedMessageContext;
import com.bt.pi.core.application.activation.ApplicationActivator;
import com.bt.pi.core.environment.KoalaEnvironment;
import com.bt.pi.core.exception.KoalaNodeInitializationException;
import com.bt.pi.core.exception.PiConfigurationException;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.pastry_override.PiSelectorManager;

public class KoalaNodeTest {
    private String nodeId = "nodeId";
    private PastryNode node;
    private File nodeIdFile;

    private KoalaNode koalaNode;

    class MyKoalaPastryApplicationBase extends KoalaPastryApplicationBase {
        private String applicationName;
        private boolean applicationShutDownCalled = false;

        public MyKoalaPastryApplicationBase(String anApplicationName) {
            applicationName = anApplicationName;
        }

        public boolean getApplicationShutDownCalled() {
            return applicationShutDownCalled;
        }

        @Override
        public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
            // TODO Auto-generated method stub

        }

        @Override
        public ApplicationActivator getApplicationActivator() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void handleNodeDeparture(String nodeId) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean becomeActive() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void becomePassive() {
            // TODO Auto-generated method stub

        }

        @Override
        public int getActivationCheckPeriodSecs() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getApplicationName() {
            return applicationName;
        }

        @Override
        public long getStartTimeout() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public TimeUnit getStartTimeoutUnit() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void onApplicationShuttingDown() {
            applicationShutDownCalled = true;
        }

    }

    @Before
    public void before() throws Exception {
        Environment riceEnvironment = mock(Environment.class);

        KoalaEnvironment environment = new KoalaEnvironment();
        environment.setLogManager(new Log4JLogManager());

        PiSelectorManager selectorManager = new PiSelectorManager();
        ReflectionTestUtils.setField(selectorManager, "inetAddress", InetAddress.getLocalHost());
        ReflectionTestUtils.setField(environment, "selectorManager", selectorManager);
        ReflectionTestUtils.setField(environment, "environment", riceEnvironment);

        Executor executor = mock(Executor.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                new Thread((Runnable) invocation.getArguments()[0]).start();
                return null;
            }
        }).when(executor).execute(isA(Runnable.class));

        koalaNode = new KoalaNode() {
            @Override
            protected void configureEnvironment() {
            }
        };
        setupNodeIdFile();
        koalaNode.setNodeIdFile(nodeId);
        koalaNode.setEnvironment(environment);
        koalaNode.setExecutor(executor);
        node = mock(PastryNode.class);
    }

    public void setupNodeIdFile() throws Exception {
        nodeIdFile = new File(nodeId);
        FileUtils.writeStringToFile(nodeIdFile, nodeId);
    }

    @After
    public void teardownNodeIdFile() {
        if (nodeIdFile.exists())
            nodeIdFile.delete();
    }

    @Test
    public void constructor() {
        assertNotNull(koalaNode);
    }

    @Test
    public void testPastryNodeSetter() {
        // act
        koalaNode.setPastryNode(node);

        // assert
        assertEquals(node, koalaNode.getPastryNode());
    }

    @Test
    public void testPortSetter() {
        // setup
        int aPort = 1232141;

        // act

        koalaNode.setPort(aPort);
        // assert
        assertEquals(aPort, koalaNode.getPort());
    }

    @Test
    public void shouldReturnNullIfNodeIdFileDoesNotExist() throws Exception {
        // setup
        teardownNodeIdFile();

        // act
        String result = koalaNode.getExistingId();

        // assert
        assertNull(result);
    }

    @Test(expected = KoalaNodeInitializationException.class)
    public void shouldThrowExceptionIfNodeIdFileThrowsIOExceptionOnRead() throws Exception {
        // setup
        nodeIdFile.setReadable(false);

        // act
        koalaNode.getExistingId();
    }

    @Test
    public void shouldUseExistingIdIfOneExists() throws Exception {
        // setup

        // act
        String result = koalaNode.getExistingId();

        // assert
        assertEquals(nodeId, result);
    }

    @Test
    public void shouldSaveNodeId() throws Exception {
        // setup

        // act
        koalaNode.saveNodeId("5467");

        // assert
        assertEquals("5467", FileUtils.readFileToString(nodeIdFile));
    }

    @Test(expected = KoalaNodeInitializationException.class)
    public void shouldThrowExceptionIfNodeIdFileThrowsIOExceptionOnWrite() throws Exception {
        // setup
        nodeIdFile.setWritable(false);

        // act
        koalaNode.saveNodeId("abc");
    }

    @Test(expected = PiConfigurationException.class)
    public void shouldThrowExceptionWithDuplicateAppNames() {
        // setup
        List<KoalaPastryApplicationBase> apps = new ArrayList<KoalaPastryApplicationBase>();
        apps.add(new KoalaPastryApplicationBase() {

            @Override
            public TimeUnit getStartTimeoutUnit() {
                return null;
            }

            @Override
            public long getStartTimeout() {
                return 0;
            }

            @Override
            public String getApplicationName() {
                return "bob";
            }

            @Override
            public int getActivationCheckPeriodSecs() {
                return 0;
            }

            @Override
            public void becomePassive() {
            }

            @Override
            public boolean becomeActive() {
                return false;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }

            @Override
            public ApplicationActivator getApplicationActivator() {
                return null;
            }

            @Override
            public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
            }
        });

        apps.add(new KoalaPastryApplicationBase() {

            @Override
            public TimeUnit getStartTimeoutUnit() {
                return null;
            }

            @Override
            public long getStartTimeout() {
                return 0;
            }

            @Override
            public String getApplicationName() {
                return "bob";
            }

            @Override
            public int getActivationCheckPeriodSecs() {
                return 0;
            }

            @Override
            public void becomePassive() {
            }

            @Override
            public boolean becomeActive() {
                return false;
            }

            @Override
            public void handleNodeDeparture(String nodeId) {
            }

            @Override
            public ApplicationActivator getApplicationActivator() {
                return null;
            }

            @Override
            public void deliver(PId id, ReceivedMessageContext receivedMessageContext) {
            }
        });

        // act
        koalaNode.setPastryApplications(apps);
    }

    @Test
    public void shouldCancelApplicationShutdownIfNotCompletedInGivenTime() {
        // setup
        koalaNode.setPastryNode(node);
        KoalaPastryApplicationBase app1 = mock(KoalaPastryApplicationBase.class);
        final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);
        final AtomicBoolean shuttingDownInvoked = new AtomicBoolean(false);

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                shuttingDownInvoked.getAndSet(true);
                Thread.sleep(5 * 1000);
                shutdownCompleted.getAndSet(true);
                return null;
            }
        }).when(app1).applicationContextShuttingDown();

        KoalaNode.APPLICATION_SHUTDOWN_TIMEOUT = 1;
        List<KoalaPastryApplicationBase> applications = new ArrayList<KoalaPastryApplicationBase>();
        applications.add(app1);

        koalaNode.setPastryApplications(applications);

        // act
        koalaNode.stop();

        // assert
        assertTrue(shuttingDownInvoked.get());
        assertFalse(shutdownCompleted.get());
        verify(node).destroy();
    }

    public void shouldShutdownApplicationsIfApplicationThrowsExceptionOnShuttingDown() {
        // setup
        koalaNode.setPastryNode(node);
        KoalaPastryApplicationBase app1 = mock(KoalaPastryApplicationBase.class);

        doThrow(new RuntimeException("Unable to complete shutdown")).when(app1).applicationContextShuttingDown();

        KoalaNode.APPLICATION_SHUTDOWN_TIMEOUT = 1;
        List<KoalaPastryApplicationBase> applications = new ArrayList<KoalaPastryApplicationBase>();
        applications.add(app1);

        koalaNode.setPastryApplications(applications);

        // act
        koalaNode.stop();

        // assert
        verify(app1).applicationContextShuttingDown();
        verify(node).destroy();
    }
}
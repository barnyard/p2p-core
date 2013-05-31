package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import rice.environment.Environment;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.Past;
import rice.pastry.PastryNode;
import rice.pastry.commonapi.PastryEndpoint;
import rice.pastry.routing.Router;

import com.bt.pi.core.application.activation.AlwaysOnApplicationActivator;
import com.bt.pi.core.application.activation.ApplicationRegistry;
import com.bt.pi.core.application.activation.InterApplicationDependenciesStore;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.KoalaDHTStorage;
import com.bt.pi.core.testing.LogHelper;
import com.bt.pi.core.testing.VectorAppender;

public class EchoApplicationTest {
    private Past past;
    private PastryNode pn;
    private rice.pastry.Id localNodeId = mock(rice.pastry.Id.class);
    private NodeHandle foriegnNodeHandle;
    private rice.pastry.NodeHandle deadNodeHandle;
    private Router router;
    private KoalaIdFactory idFactory;
    private EchoApplication echoApplication;
    private CountDownLatch applicationActivatedLatch;
    private ReceivedMessageContext messageContext;
    private InterApplicationDependenciesStore interApplicationDependenciesStore;

    @Before
    public void before() {
        applicationActivatedLatch = new CountDownLatch(1);
        interApplicationDependenciesStore = mock(InterApplicationDependenciesStore.class);

        messageContext = mock(ReceivedMessageContext.class);
        when(messageContext.getReceivedEntity()).thenReturn(new EchoPayload());

        Environment env = new Environment();

        Id foriegnId = rice.pastry.Id.build("foriegnNodeHandle");

        PId pid = mock(PId.class);
        when(pid.getIdAsHex()).thenReturn(foriegnId.toStringFull());

        idFactory = mock(KoalaIdFactory.class);
        when(idFactory.convertToPId((Id) anyObject())).thenReturn(pid);
        when(idFactory.buildId(pid)).thenReturn(foriegnId);

        foriegnNodeHandle = mock(NodeHandle.class);
        when(foriegnNodeHandle.getId()).thenReturn(foriegnId);
        rice.pastry.NodeHandle localNodeHandle = mock(rice.pastry.NodeHandle.class);
        when(localNodeHandle.getId()).thenReturn(localNodeId);
        deadNodeHandle = mock(rice.pastry.NodeHandle.class);
        when(deadNodeHandle.getId()).thenReturn(rice.pastry.Id.build("deadAsADoorNail"));
        when(deadNodeHandle.checkLiveness()).thenReturn(false);

        router = mock(Router.class);
        past = mock(KoalaDHTStorage.class);
        when(past.getEnvironment()).thenReturn(env);
        pn = mock(PastryNode.class);
        when(pn.getEnvironment()).thenReturn(env).thenReturn(env).thenReturn(env).thenReturn(env).thenReturn(env);
        when(pn.getRouter()).thenReturn(router);
        when(pn.getId()).thenReturn(localNodeId);
        when(pn.getNodeId()).thenReturn(localNodeId);
        when(pn.getLocalNodeHandle()).thenReturn(localNodeHandle);
        when(pn.buildEndpoint(isA(Application.class), isA(String.class))).thenAnswer(new Answer<PastryEndpoint>() {

            @Override
            public PastryEndpoint answer(InvocationOnMock invocation) throws Throwable {
                return new PastryEndpoint(pn, (Application) invocation.getArguments()[0], (String) invocation.getArguments()[1], true);
            }

        });

        KoalaPiEntityFactory koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(new KoalaJsonParser());
        ArrayList<PiEntity> applicationPayloads = new ArrayList<PiEntity>();
        applicationPayloads.add(new EchoPayload());
        koalaPiEntityFactory.setPiEntityTypes(applicationPayloads);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.initialize();

        AlwaysOnApplicationActivator aa = new AlwaysOnApplicationActivator();
        aa.setScheduledExecutorService(Executors.newScheduledThreadPool(4));
        aa.setApplicationRegistry(new ApplicationRegistry());
        aa.setExecutor(executor);

        echoApplication = new EchoApplication() {
            @Override
            public boolean becomeActive() {
                boolean res = super.becomeActive();
                applicationActivatedLatch.countDown();
                return res;
            }
        };

        echoApplication.setApplicationActivator(aa);
        echoApplication.setKoalaPiEntityFactory(koalaPiEntityFactory);
        echoApplication.setKoalaJsonParser(new KoalaJsonParser());
        echoApplication.setKoalaIdFactory(idFactory);
        ReflectionTestUtils.setField(aa, "interApplicationDependenciesStore", interApplicationDependenciesStore);
        LogHelper.initLogging();
    }

    @After
    public void after() {
        LogHelper.resetLogging();
    }

    @Test
    public void testStart() throws Exception {
        LogHelper.resetLogging();
        // act
        echoApplication.start(pn, (KoalaDHTStorage) past, null, null);

        // assert
        assertTrue(applicationActivatedLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testDeliver() {
        LogHelper.resetLogging();

        // act
        echoApplication.deliver(new PiId(rice.pastry.Id.build("someId").toStringFull(), 0), messageContext);

        // assert
        assertTrue(LogHelper.containsString(VectorAppender.getMessages(), "EchoPayload"));
        assertTrue(LogHelper.containsString(VectorAppender.getMessages(), "received"));
    }

    @Test
    public void getApplicationName() {
        assertEquals("pi-echo-application", echoApplication.getApplicationName());
    }
}

package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import rice.Continuation;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.message.KoalaMessageBase;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.messaging.KoalaMessageContinuation;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class MessageContextTest {
    private static final String AAAAAABAD = "AAAAAABAD";
    private static final String TRANSACTION_UID = "transaction-uid";
    private MessageContext messageContext;
    private KoalaPastryApplicationBase handlingApp;
    private Id handlingNodeId;
    private PId someId;
    private KoalaMessageBase koalaMessage;
    private Endpoint endpoint;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private KoalaIdFactory koalaIdFactory;
    private KoalaJsonParser koalaJsonParser;
    private PiEntity piEntity;
    private String json = "json";
    private ContinuationRequestWrapperImpl continuationRequestWrapper;
    @SuppressWarnings("unchecked")
    private KoalaMessageContinuation koalaMessageContinuation;

    @Before
    public void before() {
        koalaMessageContinuation = mock(KoalaMessageContinuation.class);
        endpoint = mock(Endpoint.class);
        someId = mock(PId.class);
        when(someId.getIdAsHex()).thenReturn(AAAAAABAD);
        when(someId.toStringFull()).thenReturn(AAAAAABAD);
        handlingNodeId = mock(Id.class);
        piEntity = mock(PiEntity.class);
        koalaMessage = mock(KoalaMessageBase.class);
        koalaJsonParser = mock(KoalaJsonParser.class);
        continuationRequestWrapper = mock(ContinuationRequestWrapperImpl.class);

        koalaPiEntityFactory = new KoalaPiEntityFactory();
        koalaPiEntityFactory.setKoalaJsonParser(koalaJsonParser);

        when(koalaJsonParser.getJson(piEntity)).thenReturn(json);

        koalaIdFactory = new KoalaIdFactory(0, 0);

        handlingApp = mock(KoalaPastryApplicationBase.class);
        when(handlingApp.getApplicationName()).thenReturn("app");
        when(handlingApp.getNodeId()).thenReturn(handlingNodeId);
        when(handlingApp.getEndpoint()).thenReturn(endpoint);
        when(handlingApp.getKoalaPiEntityFactory()).thenReturn(koalaPiEntityFactory);
        when(handlingApp.getKoalaJsonParser()).thenReturn(koalaJsonParser);
        when(handlingApp.getContinuationRequestWrapper()).thenReturn(continuationRequestWrapper);
        when(handlingApp.getKoalaIdFactory()).thenReturn(koalaIdFactory);

        messageContext = new MessageContext(handlingApp) {
            @SuppressWarnings("unchecked")
            @Override
            protected <T extends PiEntity> KoalaMessageContinuation<T> getKoalaMessageContinuation(Continuation<T, Exception> appContinuation) {
                return koalaMessageContinuation;
            }
        };
    }

    @Test
    public void shouldRouteMessageViaEndpiint() {
        // act
        messageContext.routeMessage(someId, koalaMessage);

        // verify
        verify(endpoint).route(eq(koalaIdFactory.buildId(someId)), eq(koalaMessage), (NodeHandle) eq(null));
        verify(koalaMessage).setKoalaJsonParser(koalaJsonParser);
    }

    @Test
    public void shouldRouteMessageViaEndpiintWhenContinuationNull() {
        // act
        messageContext.routeMessage(someId, koalaMessage, null);

        // verify
        verify(endpoint).route(eq(koalaIdFactory.buildId(someId)), eq(koalaMessage), (NodeHandle) eq(null));
        verify(koalaMessage).setKoalaJsonParser(koalaJsonParser);
    }

    @Test
    public void shouldRoutePayloadToAnotherApp() {
        // setup
        messageContext = new MessageContext(handlingApp) {
            @SuppressWarnings("unchecked")
            @Override
            protected <T extends PiEntity> KoalaMessageContinuation<T> getKoalaMessageContinuation(Continuation<T, Exception> appContinuation) {
                return koalaMessageContinuation;
            }

            @Override
            protected ApplicationMessage createApplicationMessage(EntityMethod method, EntityResponseCode entityResponseCode, PiEntity payload, String destAppName, String sourceAppName) {
                assertEquals("app", sourceAppName);
                assertEquals("some-other-app", destAppName);
                return super.createApplicationMessage(method, entityResponseCode, payload, destAppName, sourceAppName);
            }
        };

        // act
        messageContext.routePiMessageToApplication(someId, EntityMethod.GET, piEntity, "some-other-app");

        // verify
        verify(endpoint).route(eq(koalaIdFactory.buildId(someId)), argThat(new ArgumentMatcher<ApplicationMessage>() {
            public boolean matches(Object argument) {
                ApplicationMessage arg = (ApplicationMessage) argument;
                assertTrue(arg.getTransactionUID().length() > 0);
                assertFalse(TRANSACTION_UID.equals(arg.getTransactionUID()));
                return true;
            }
        }), (NodeHandle) eq(null));
    }

    @Test
    public void shouldRoutePayloadInTransaction() {
        // setup
        messageContext = new MessageContext(handlingApp, TRANSACTION_UID);

        // act
        messageContext.routePiMessageToApplication(someId, EntityMethod.GET, piEntity, "some-other-app");

        // verify
        verify(endpoint).route(eq(koalaIdFactory.buildId(someId)), argThat(new ArgumentMatcher<ApplicationMessage>() {
            public boolean matches(Object argument) {
                ApplicationMessage arg = (ApplicationMessage) argument;
                assertEquals(TRANSACTION_UID, arg.getTransactionUID());
                return true;
            }
        }), (NodeHandle) eq(null));
    }

    @Test
    public void shouldRoutePayloadViaEndpiint() {
        // act
        messageContext.routePiMessage(someId, EntityMethod.GET, piEntity);

        // verify
        verify(endpoint).route(eq(koalaIdFactory.buildId(someId)), isA(ApplicationMessage.class), (NodeHandle) eq(null));
    }

    @Test
    public void shouldRouteMessageDirectViaEndpiint() {
        // setup
        NodeHandle nh = mock(NodeHandle.class);

        // act
        messageContext.routeMsgDirect(nh, koalaMessage);

        // verify
        verify(endpoint).route((Id) eq(null), eq(koalaMessage), eq(nh));
    }

    @Test
    public void shouldRoutePayloadDirectViaEndpiint() {
        // setup
        NodeHandle nh = mock(NodeHandle.class);

        // act
        messageContext.routeMsgDirect(nh, EntityMethod.GET, piEntity);

        // verify
        verify(endpoint).route((Id) eq(null), isA(ApplicationMessage.class), eq(nh));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRouteMessageThroughContinuation() {
        // setup
        Continuation<KoalaMessage, Exception> appContinuation = mock(Continuation.class);

        // act
        messageContext.routeMessage(someId, koalaMessage, appContinuation);

        // verify
        verify(continuationRequestWrapper).sendRequest(someId, koalaMessage, messageContext, appContinuation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRoutePayloadThroughContinuation() {
        // setup
        Continuation<PiEntity, Exception> appContinuation = mock(Continuation.class);

        // act
        messageContext.routePiMessage(someId, EntityMethod.GET, piEntity, appContinuation);

        // verify
        verify(continuationRequestWrapper).sendRequest(eq(someId), isA(KoalaMessage.class), eq(messageContext), eq(koalaMessageContinuation));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRoutePayloadThroughContinuationToAnotherApp() {
        // setup
        Continuation<PiEntity, Exception> appContinuation = mock(Continuation.class);

        messageContext = new MessageContext(handlingApp) {
            @Override
            protected <T extends PiEntity> KoalaMessageContinuation<T> getKoalaMessageContinuation(Continuation<T, Exception> appContinuation) {
                return koalaMessageContinuation;
            }

            @Override
            protected ApplicationMessage createApplicationMessage(EntityMethod method, EntityResponseCode entityResponseCode, PiEntity payload, String destAppName, String sourceAppName) {
                assertEquals("app", sourceAppName);
                assertEquals("some-other-app", destAppName);
                return super.createApplicationMessage(method, entityResponseCode, payload, destAppName, sourceAppName);
            }
        };

        // act
        messageContext.routePiMessageToApplication(someId, EntityMethod.GET, piEntity, "some-other-app", appContinuation);

        // verify
        verify(continuationRequestWrapper).sendRequest(eq(someId), isA(KoalaMessage.class), eq(messageContext), eq(koalaMessageContinuation));
    }
}

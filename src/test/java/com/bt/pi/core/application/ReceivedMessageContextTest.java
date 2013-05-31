package com.bt.pi.core.application;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.messaging.ContinuationRequestWrapperImpl;
import com.bt.pi.core.parser.KoalaJsonParser;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class ReceivedMessageContextTest {
    private static final String RECEIVED_MESSAGE_ID = "RECEIVED_MESSAGE_ID";
    private static final String HANDLING_NODE_ID = "FULL_ID";
    private static final String CORRELATION_UID = "correlation-uid";
    private static final String TRANSACTION_UID = "transaction-uid";
    private ReceivedMessageContext messageContext;
    private ApplicationMessage applicationMessage;
    private KoalaPastryApplicationBase handlingApp;
    private Id handlingNodeId;
    private Id responseId;
    private PId receivedMessageId;
    private Endpoint endpoint;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private KoalaJsonParser koalaJsonParser;
    private PiEntity piEntity;
    private String json = "json";
    private ContinuationRequestWrapperImpl continuationRequestWrapper;
    private ApplicationMessage createdApplicationMessage;
    private KoalaIdFactory idFactory;

    @Before
    public void before() {
        endpoint = mock(Endpoint.class);
        receivedMessageId = mock(PId.class);
        responseId = mock(Id.class);
        handlingNodeId = mock(Id.class);
        piEntity = mock(PiEntity.class);
        koalaJsonParser = mock(KoalaJsonParser.class);
        idFactory = mock(KoalaIdFactory.class);
        applicationMessage = mock(ApplicationMessage.class);
        continuationRequestWrapper = mock(ContinuationRequestWrapperImpl.class);

        koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);
        when(koalaPiEntityFactory.getPiEntity(json)).thenReturn(piEntity);

        when(handlingNodeId.toStringFull()).thenReturn(HANDLING_NODE_ID);

        when(koalaJsonParser.getJson(piEntity)).thenReturn(json);

        when(idFactory.buildPIdFromHexString(RECEIVED_MESSAGE_ID)).thenReturn(receivedMessageId);
        when(idFactory.buildId(receivedMessageId)).thenReturn(responseId);

        when(applicationMessage.getJson()).thenReturn(json);
        when(applicationMessage.getMethod()).thenReturn(EntityMethod.CREATE);
        when(applicationMessage.getResponseCode()).thenReturn(EntityResponseCode.OK);
        when(applicationMessage.getSourceApplicationName()).thenReturn("source-app");
        when(applicationMessage.getSourceId()).thenReturn(RECEIVED_MESSAGE_ID);
        when(applicationMessage.getCorrelationUID()).thenReturn(CORRELATION_UID);
        when(applicationMessage.getTransactionUID()).thenReturn(TRANSACTION_UID);

        handlingApp = mock(KoalaPastryApplicationBase.class);
        when(handlingApp.getApplicationName()).thenReturn("app");
        when(handlingApp.getNodeId()).thenReturn(handlingNodeId);
        when(handlingApp.getEndpoint()).thenReturn(endpoint);
        when(handlingApp.getKoalaPiEntityFactory()).thenReturn(koalaPiEntityFactory);
        when(handlingApp.getKoalaIdFactory()).thenReturn(idFactory);
        when(handlingApp.getKoalaJsonParser()).thenReturn(koalaJsonParser);
        when(handlingApp.getContinuationRequestWrapper()).thenReturn(continuationRequestWrapper);

        messageContext = new ReceivedMessageContext(handlingApp, applicationMessage);
    }

    @Test
    public void shouldGetReceivedMessageParams() {
        // act & assert
        assertEquals(EntityMethod.CREATE, messageContext.getMethod());
        assertEquals(EntityResponseCode.OK, messageContext.getResponseCode());
        assertEquals(handlingApp, messageContext.getHandlingApplication());
        assertEquals(piEntity, messageContext.getReceivedEntity());
    }

    @Test
    public void shouldBeAbleToSendMessageWithSameCorrelationIdAsReceivedMessage() {
        // setup
        messageContext = new ReceivedMessageContext(handlingApp, applicationMessage) {
            @Override
            protected ApplicationMessage createApplicationMessage(EntityMethod method, EntityResponseCode entityResponseCode, PiEntity payload, String destAppName, String sourceAppName) {
                createdApplicationMessage = super.createApplicationMessage(method, entityResponseCode, payload, destAppName, sourceAppName);
                assertEquals("app", createdApplicationMessage.getSourceApplicationName());
                assertEquals("source-app", createdApplicationMessage.getDestinationApplicationName());
                assertEquals(HANDLING_NODE_ID, createdApplicationMessage.getSourceId());
                return createdApplicationMessage;
            }
        };

        // act
        messageContext.sendResponse(EntityResponseCode.OK, piEntity);

        // verify
        verify(endpoint).route(eq(responseId), eq(createdApplicationMessage), (NodeHandle) eq(null));
        assertEquals(koalaJsonParser, createdApplicationMessage.getKoalaJsonParser());
        assertEquals(CORRELATION_UID, createdApplicationMessage.getCorrelationUID());
        assertEquals(TRANSACTION_UID, createdApplicationMessage.getTransactionUID());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToSendResponseWithoutResponseCode() {
        // setup
        messageContext = new ReceivedMessageContext(handlingApp, applicationMessage) {
            @Override
            protected ApplicationMessage createApplicationMessage(EntityMethod method, EntityResponseCode entityResponseCode, PiEntity payload, String destAppName, String sourceAppName) {
                createdApplicationMessage = super.createApplicationMessage(method, entityResponseCode, payload, destAppName, sourceAppName);
                assertEquals("app", createdApplicationMessage.getSourceApplicationName());
                assertEquals("source-app", createdApplicationMessage.getDestinationApplicationName());
                assertEquals(HANDLING_NODE_ID, createdApplicationMessage.getSourceId());
                return createdApplicationMessage;
            }
        };

        // act
        messageContext.sendResponse(null, piEntity);
    }
}

package com.bt.pi.core.messaging;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.message.ApplicationMessage;
import com.bt.pi.core.parser.KoalaPiEntityFactory;

public class KoalaMessageContinuationTest {
    KoalaMessageContinuation<PiEntity> koalaMessageContinuation;
    private KoalaPiEntityFactory koalaPiEntityFactory;
    private Continuation<PiEntity, Exception> piEntityContinuation;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        piEntityContinuation = mock(Continuation.class);
        koalaPiEntityFactory = mock(KoalaPiEntityFactory.class);

        koalaMessageContinuation = new KoalaMessageContinuation<PiEntity>(piEntityContinuation, koalaPiEntityFactory);
    }

    @Test
    public void shouldPassContinuationToPiEntityContinuation() throws Exception {
        // setup
        Exception mockException = mock(Exception.class);

        // act
        koalaMessageContinuation.receiveException(mockException);
        Thread.sleep(300); // temporary until we lose multithreadedcontinuation

        // assert
        verify(piEntityContinuation).receiveException(mockException);
    }

    @Test
    public void shouldUseKoalaPiEntityFactoryToTransformPiEntityIntoKoalaMessage() throws Exception {
        final String json = "json";

        @SuppressWarnings("serial")
        // setup
        class MyKoalaMessage extends ApplicationMessage {
            @Override
            public String getJson() {
                return json;
            }

            @Override
            public EntityResponseCode getResponseCode() {
                return EntityResponseCode.OK;
            }
        }

        MyKoalaMessage myKoalaMessage = new MyKoalaMessage();

        PiEntity mockPiEntity = mock(PiEntity.class);
        when(koalaPiEntityFactory.getPiEntity(json)).thenReturn(mockPiEntity);

        // act
        koalaMessageContinuation.receiveResult(myKoalaMessage);
        Thread.sleep(300); // temporary until we lose multithreadedcontinuation

        // assert
        verify(piEntityContinuation, never()).receiveException(isA(RuntimeException.class));
        verify(piEntityContinuation).receiveResult(mockPiEntity);
    }
}

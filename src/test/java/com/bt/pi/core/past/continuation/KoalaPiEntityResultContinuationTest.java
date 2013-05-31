package com.bt.pi.core.past.continuation;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import rice.Continuation;

import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.KoalaPiEntityContent;

public class KoalaPiEntityResultContinuationTest {
    private KoalaPiEntityResultContinuation<PiEntity> continuation;
    private Continuation<PiEntity, Exception> piEnContinuation;
    private KoalaPiEntityFactory piEntityFactory;
    private Exception lastException;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        lastException = null;

        piEnContinuation = mock(Continuation.class);
        piEntityFactory = mock(KoalaPiEntityFactory.class);
        continuation = new KoalaPiEntityResultContinuation<PiEntity>(piEnContinuation, piEntityFactory);
    }

    @Test
    public void testRecieveException() {
        // setup
        NullPointerException exception = new NullPointerException();

        // act
        continuation.receiveException(exception);

        // verify
        verify(piEnContinuation).receiveException(eq(exception));
    }

    @Test
    public void testReceiveResult() {
        // setup
        KoalaPiEntityContent content = mock(KoalaPiEntityContent.class);
        when(content.getBody()).thenReturn("dude");
        PiEntity entity = new EchoPayload();
        when(piEntityFactory.getPiEntity(eq("dude"))).thenReturn(entity);

        // act
        continuation.receiveResult(content);

        // verify
        verify(piEnContinuation).receiveResult(eq(entity));
    }

    @Test
    public void testReceiveResultWithUnexpectedEntity() {
        // setup
        Continuation<DummyEntity, Exception> piEnContinuation = new Continuation<DummyEntity, Exception>() {
            @Override
            public void receiveException(Exception exception) {
                lastException = exception;
            }

            @Override
            public void receiveResult(DummyEntity result) {
            }

        };
        KoalaPiEntityResultContinuation<DummyEntity> continuation = new KoalaPiEntityResultContinuation<DummyEntity>(piEnContinuation, piEntityFactory);

        KoalaPiEntityContent content = mock(KoalaPiEntityContent.class);
        when(content.getBody()).thenReturn("dude");
        PiEntity entity = new EchoPayload();
        when(piEntityFactory.getPiEntity(eq("dude"))).thenReturn(entity);

        // act
        continuation.receiveResult(content);

        // verify
        assertTrue(lastException instanceof ClassCastException);
    }

    /*
     * Nulls should be passed to the upper continuation.
     */
    @Test
    public void testReceiveResultIsNull() {
        // act
        continuation.receiveResult(null);

        // verify
        verify(piEnContinuation).receiveResult((PiEntity) isNull());
    }

    public class DummyEntity extends PiEntityBase {
        @Override
        public String getType() {
            return "dummy";
        }

        @Override
        public String getUrl() {
            return "url:abc";
        }

        @Override
        public String getUriScheme() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}

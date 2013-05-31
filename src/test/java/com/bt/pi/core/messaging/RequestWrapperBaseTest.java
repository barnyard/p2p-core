package com.bt.pi.core.messaging;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.id.PId;
import com.bt.pi.core.message.KoalaMessage;

public class RequestWrapperBaseTest {
    private RequestWrapperBase<Object, KoalaMessage> requestWrapperBase;

    @Before
    public void before() {
        requestWrapperBase = new RequestWrapperBase<Object, KoalaMessage>() {
            @Override
            public boolean messageReceived(PId id, KoalaMessage message) {
                // TODO Auto-generated method stub
                return false;
            }
        };
    }

    @Test
    public void shouldStartWithDefaultTimeout() {
        // act
        long timeout = requestWrapperBase.getRequestTimeoutMillis();

        // assert
        assertEquals(RequestWrapperBase.DEFAULT_REQUEST_TIMEOUT, timeout);
    }

    @Test
    public void shouldBeAbleToSetDefaultTimeout() {
        // act
        requestWrapperBase.setRequestTimeoutMillis(999);

        // assert
        assertEquals(999L, requestWrapperBase.getRequestTimeoutMillis());
    }
}

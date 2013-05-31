package com.bt.pi.core.message;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;

import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.parser.KoalaJsonParser;

public class KoalaMessageBaseTest {
    private KoalaMessageBase koalaMessageBase;
    private OutputBuffer outputBuffer;
    private KoalaJsonParser koalaJsonParser;

    @Before
    public void before() {
        outputBuffer = mock(OutputBuffer.class);
        koalaJsonParser = new KoalaJsonParser();
        koalaMessageBase = new KoalaMessageBase("test-type") {
            private static final long serialVersionUID = 1L;

            @Override
            public EntityResponseCode getResponseCode() {
                return null;
            }
        };
        koalaMessageBase.setKoalaJsonParser(koalaJsonParser);
    }

    /**
     * Should be able to serialize message
     */
    @Test
    public void shouldBeAbleToSerializeMessageIfOutputBufferIsNotDataOutputStream() throws Exception {
        // setup
        final String jsonString = koalaJsonParser.getJson(koalaMessageBase);

        // act
        koalaMessageBase.serialize(outputBuffer);

        // assert
        verify(outputBuffer).writeInt(jsonString.length());
        verify(outputBuffer).write(argThat(new ArgumentMatcher<byte[]>() {
            @Override
            public boolean matches(Object argument) {
                return new String((byte[]) argument).equals(jsonString);
            }
        }), eq(0), eq(jsonString.length()));
    }

    /**
     * Should be able to set cx and tx ids
     */
    @Test
    public void shouldBeAbleToSetCorrelationAndTransactionIds() throws Exception {
        // act
        koalaMessageBase.setCorrelationUID("1");
        koalaMessageBase.setTransactionUID("2");

        // assert
        assertEquals("1", koalaMessageBase.getCorrelationUID());
        assertEquals("2", koalaMessageBase.getTransactionUID());
    }

    @Test
    public void shouldSerializeLargeMessages() throws Exception {
        // setup
        class MyKoalaMessageBase extends KoalaMessageBase {
            private static final long serialVersionUID = 1L;
            private String reallyLongString;

            public MyKoalaMessageBase() {
                super("mykoalamessagebase");
                setKoalaJsonParser(koalaJsonParser);

                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < 70000; i++)
                    stringBuilder.append("a");
                reallyLongString = stringBuilder.toString();
            }

            @Override
            public EntityResponseCode getResponseCode() {
                return null;
            }

            @SuppressWarnings("unused")
            public String getReallyLongString() {
                return reallyLongString;
            }
        }
        koalaMessageBase = new MyKoalaMessageBase();
        outputBuffer = new SimpleOutputBuffer();

        // act
        koalaMessageBase.serialize(outputBuffer);

        // assert
        String json = koalaJsonParser.getJson(koalaMessageBase);
        byte[] bytes = ((SimpleOutputBuffer) outputBuffer).getBytes();
        // assertEquals(json.length(), bytesLength);
        assertEquals(json, new String(ArrayUtils.subarray(bytes, 4, json.length() + 4)));
    }
}

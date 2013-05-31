package com.bt.pi.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.rawserialization.InputBuffer;

import com.bt.pi.core.message.KoalaMessage;

public class KoalaMessageDeserializerTest {
	private KoalaMessageDeserializer koalaMessageDeserializer;
	private InputBuffer inputBuffer;
	private KoalaMessageFactory koalaMessageFactory;
	private KoalaMessage koalaMessage;

	@Before
	public void before() throws Exception {
        final String messageString = "{\"test\":\"is best\"}";

		inputBuffer = mock(InputBuffer.class);
        when(inputBuffer.readInt()).thenReturn(messageString.length());
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] bytes = messageString.getBytes();
                for (int i = 0; i < bytes.length; i++)
                    ((byte[]) invocation.getArguments()[0])[i] = bytes[i];
                return messageString.length();
            }
        }).when(inputBuffer).read(isA(byte[].class));

		koalaMessage = mock(KoalaMessage.class);

		koalaMessageFactory = mock(KoalaMessageFactory.class);
		when(koalaMessageFactory.createMessage(isA(JSONObject.class))).thenReturn(koalaMessage);

		koalaMessageDeserializer = new KoalaMessageDeserializer();
		koalaMessageDeserializer.setKoalaMessageFactory(koalaMessageFactory);
	}

	/**
	 * Should deserialize message via factory
	 */
	@Test
	public void shouldDeserializeMessageViaFactory() throws Exception {
		// act
		Message res = koalaMessageDeserializer.deserialize(inputBuffer, (short) 0, 0, null);

		// assert
		assertEquals(res, koalaMessage);
	}
}

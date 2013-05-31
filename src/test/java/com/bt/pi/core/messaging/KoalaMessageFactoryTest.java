package com.bt.pi.core.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.exception.MessageCreationException;
import com.bt.pi.core.message.ExampleMessage;
import com.bt.pi.core.message.KoalaMessage;
import com.bt.pi.core.message.KoalaMessageBase;
import com.bt.pi.core.messaging.KoalaMessageFactory;
import com.bt.pi.core.messaging.KoalaMessageFactoryImpl;
import com.bt.pi.core.parser.KoalaJsonParser;

public class KoalaMessageFactoryTest {
    private KoalaMessageFactory koalaMessageFactory;
    private JSONObject jsonObject;

    @Before
    public void before() throws Exception {
        jsonObject = new JSONObject(new KoalaJsonParser().getJson(new ExampleMessage()));

        this.koalaMessageFactory = new KoalaMessageFactoryImpl();

        List<KoalaMessageBase> messages = new ArrayList<KoalaMessageBase>();
        messages.add(new ExampleMessage());
        ((KoalaMessageFactoryImpl) koalaMessageFactory).setApplicationMessageTypes(messages);
        ((KoalaMessageFactoryImpl) koalaMessageFactory).setKoalaJsonParser(new KoalaJsonParser());
    }

    /**
     * Should be able to create a message from JSON
     */
    @Test
    public void shouldCreateKnownMessage() {
        // act
        KoalaMessage res = koalaMessageFactory.createMessage(jsonObject);

        // assert
        assertTrue(res instanceof ExampleMessage);
        assertEquals("se", ((ExampleMessage) res).getMoo());
    }

    /**
     * Should fail to create message when no type
     */
    @Test(expected = MessageCreationException.class)
    public void shouldFailToCreateMessageWhenNoType() {
        // act
        koalaMessageFactory.createMessage(new JSONObject());
    }

    /**
     * Should fail to create message for bad type
     */
    @Test(expected = MessageCreationException.class)
    public void shouldFailToCreateMessageForBadType() throws Exception {
        // setup
        jsonObject.put("koalaMessageType", "unknown");

        // act
        koalaMessageFactory.createMessage(jsonObject);
    }

}
package com.bt.pi.core.message;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import rice.pastry.Id;

import com.bt.pi.core.application.EchoApplication;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.message.ApplicationMessage;

public class ApplicationMessageTest {
    private EchoApplication echoApp = new EchoApplication();
	private ApplicationMessage appMessage;
    
    @Before
    public void before() {
    	appMessage = new ApplicationMessage("json", null, null, null, null, null);
    }
    
    @Test
    public void testGettersAndSetters() {
        // setup
    	String cool = "cool";
        
    	// act
        appMessage.setJson(cool);
        appMessage.setSourceId(Id.build(cool).toStringFull());
        appMessage.setResponseCode(EntityResponseCode.ERROR);
        appMessage.setMethod(EntityMethod.CREATE);
        appMessage.setDestinationApplicationName(echoApp.getApplicationName());
        appMessage.setSourceApplicationName(echoApp.getApplicationName());

        // assert
        assertEquals(cool, appMessage.getJson());
        assertEquals(Id.build(cool).toStringFull(), appMessage.getSourceId());
        assertEquals(EntityResponseCode.ERROR, appMessage.getResponseCode());
        assertEquals(EntityMethod.CREATE, appMessage.getMethod());
        assertEquals(echoApp.getApplicationName(), appMessage.getDestinationApplicationName());
        assertEquals(echoApp.getApplicationName(), appMessage.getSourceApplicationName());

    }
}

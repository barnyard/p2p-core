package com.bt.pi.core;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.EchoApplication;
import com.bt.pi.core.application.MessageContext;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.id.PiId;
import com.bt.pi.core.message.payload.EchoPayload;
import com.bt.pi.core.testing.BeanPropertiesMunger;
import com.bt.pi.core.testing.RingHelper;

public class MessagingIntegrationTest extends RingHelper {
    private int numberOfNodes;
    private EchoApplication currentEchoApp;

    @Before
    public void before() throws Exception {
        BeanPropertiesMunger.setDoMunging(true);
        numberOfNodes = 2;
        super.before(numberOfNodes, null);
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    // where is the assert for this test?
    @Test
    public void testSimpleMessageRouting() throws Exception {
        // setup
        changeContextToNode(0);
        PId zerothNodeId = new PiId(currentPastryNode.getId().toStringFull(), 0);

        EchoPayload payload = new EchoPayload();

        // actrealTestId
        changeContextToNode(1);
        MessageContext messageContext = currentEchoApp.newMessageContext();
        messageContext.routePiMessage(zerothNodeId, EntityMethod.CREATE, payload);

        Thread.sleep(5000);
    }

    @Override
    public void updateLocalsForNodeContextChange(Map<String, Object> currentApplicationMap) {
        currentEchoApp = (EchoApplication) currentApplicationContext.getBean("echoApplication");
    }
}

package com.bt.pi.core.scribe.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.NodeHandle;

import com.bt.pi.core.entity.EntityMethod;

public class KoalaScribeContentTest {

    private JSONObject json;
    private NodeHandle nodeHandle;
    private KoalaScribeContent koalaScribeContent;
    private String transactionUID;

    @Before
    public void before() throws Exception {
        transactionUID = UUID.randomUUID().toString();
        json = new JSONObject();
        json.put("my sandwich", "down");
        nodeHandle = mock(NodeHandle.class);
        koalaScribeContent = new KoalaScribeContent(nodeHandle, transactionUID, EntityMethod.CREATE, json);
    }

    @Test
    public void testContstructor() {
        // act
        koalaScribeContent = new KoalaScribeContent(nodeHandle, transactionUID, EntityMethod.CREATE, json);

        // assert
        assertEquals(json.toString(), koalaScribeContent.getJsonData());
        assertEquals(nodeHandle, koalaScribeContent.getSourceNodeHandle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void contstructorShouldBarfOnNullEntityMethod() {
        // act
        koalaScribeContent = new KoalaScribeContent(nodeHandle, transactionUID, null, json);
    }

    @Test
    public void testGettersAndSetters() {
        NodeHandle otherHandle = mock(NodeHandle.class);
        // act
        koalaScribeContent.setSourceNodeHandle(otherHandle);

        assertEquals(otherHandle, koalaScribeContent.getSourceNodeHandle());
    }
}

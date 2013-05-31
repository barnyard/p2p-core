package com.bt.pi.core.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import rice.pastry.Id;
import rice.pastry.NodeHandle;

import com.bt.pi.core.node.KoalaNode;

public class NodeManagementTest {
    NodeManagement nodeManagement;
    KoalaNode koalaNode;
    NodeHandle mockHandle1, mockHandle2;

    @Before
    public void before() {

        mockHandle1 = mock(NodeHandle.class);
        when(mockHandle1.getId()).thenReturn(Id.build("wow"));
        when(mockHandle1.toString()).thenReturn("hellooooo");

        mockHandle2 = mock(NodeHandle.class);
        when(mockHandle2.getId()).thenReturn(Id.build("cool"));
        when(mockHandle2.toString()).thenReturn("hellooooo2");

        ArrayList<NodeHandle> handles = new ArrayList<NodeHandle>();
        handles.add(mockHandle1);
        handles.add(mockHandle2);

        koalaNode = mock(KoalaNode.class);
        when(koalaNode.getLeafNodeHandles()).thenReturn(handles);
        when(koalaNode.getLocalNodeHandle()).thenReturn(mockHandle1);

        nodeManagement = new NodeManagement();
        nodeManagement.setNode(koalaNode);

    }

    @Test
    public void testGetLeafSet() {
        // act
        Collection<String> leafset = nodeManagement.getLeafSet();

        // assert
        assertEquals(2, leafset.size());
        assertTrue("String did not contain mockhandle1", leafset.toString().contains(mockHandle1.toString()));
        assertTrue("String did not contain mockhandle1 id", leafset.toString().contains(mockHandle1.getId().toStringFull()));
        assertTrue("String did not contain mockhandle2", leafset.toString().contains(mockHandle2.toString()));
        assertTrue("String did not contain mockhandle2 id", leafset.toString().contains(mockHandle2.getId().toStringFull()));
    }

    @Test
    public void testGetLocalNodeHandle() {
        // act
        String result = nodeManagement.getLocalNodeHandle();

        // assert
        assertTrue("String did not contain mockhandle1", result.contains(mockHandle1.toString()));
        assertTrue("String did not contain mockhandle1 id", result.contains(mockHandle1.getId().toStringFull()));
    }

}

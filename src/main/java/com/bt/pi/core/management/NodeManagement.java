package com.bt.pi.core.management;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import rice.pastry.NodeHandle;

import com.bt.pi.core.node.KoalaNode;

@ManagedResource(description = "An interface for checking node status", objectName = "bean:name=nodeManagement")
@Component
public class NodeManagement {
    private static final String NODEHANDLE_FORMAT = "%s-%s";
    private KoalaNode koalaNode;

    public NodeManagement() {
        koalaNode = null;
    }

    @Resource
    public void setNode(KoalaNode node) {
        koalaNode = node;
    }

    @ManagedOperation(description = "Returns a collection of the nodehandles in the nodes leafset.")
    public Collection<String> getLeafSet() {
        Collection<NodeHandle> leafSetHandles = koalaNode.getLeafNodeHandles();
        Collection<String> leafSet = new ArrayList<String>();
        for (NodeHandle nh : leafSetHandles) {
            if (nh != null) {
                leafSet.add(String.format(NODEHANDLE_FORMAT, nh.getId().toStringFull(), nh.toString()));
            }
        }
        return leafSet;
    }

    @ManagedOperation(description = "Returns the nodehandle for the localNode.")
    public String getLocalNodeHandle() {
        return String.format(NODEHANDLE_FORMAT, koalaNode.getLocalNodeHandle().getId().toStringFull(), koalaNode.getLocalNodeHandle().toString());
    }
}

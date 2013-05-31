package com.bt.pi.core.application;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rice.p2p.commonapi.Id;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;

import com.bt.pi.core.application.activation.ActivationAwareApplication;

/**
 * Provides a starting point for classes intending to implement {@link ActivationAwareApplication}.
 * 
 * TODO: extract PastryNode and related methods to a PastyNodeWrapper class
 */
public abstract class ActivationAwareApplicationBase implements ActivationAwareApplication {

    private PastryNode pastryNode;

    protected ActivationAwareApplicationBase() {
        this.pastryNode = null;
    }

    protected void start(PastryNode aPastryNode) {
        this.pastryNode = aPastryNode;
    }

    @Override
    public List<String> getPreferablyExcludedApplications() {
        return Collections.emptyList();
    }

    /**
     * Returns the pastry Id of the current node.
     */
    @Override
    public Id getNodeId() {
        return getPastryNode().getId();
    }

    /**
     * Returns a collection of unique NodeHandles in the leafset of the current node.
     */
    @Override
    public Collection<NodeHandle> getLeafNodeHandles() {
        return getPastryNode().getLeafSet().getUniqueSet();
    }

    // TODO: encapsulate PastryNode
    protected PastryNode getPastryNode() {
        return this.pastryNode;
    }
}

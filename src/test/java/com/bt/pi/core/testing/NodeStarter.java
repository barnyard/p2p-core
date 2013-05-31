package com.bt.pi.core.testing;

import com.bt.pi.core.node.KoalaNode;

public class NodeStarter implements Runnable {
    private KoalaNode node;
    private boolean shouldCreateApps;

    public NodeStarter(KoalaNode koalaNode, boolean createApps) {
        node = koalaNode;
        shouldCreateApps = createApps;
    }

    @Override
    public void run() {
        if (shouldCreateApps) {
            node.start();
        } else
            node.createPastryNode(null);
    }
}
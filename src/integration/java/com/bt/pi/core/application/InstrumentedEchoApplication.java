package com.bt.pi.core.application;

import java.util.HashMap;

public class InstrumentedEchoApplication extends EchoApplication {

    public static HashMap<String, String> departedNodesHash = new HashMap<String, String>();
    public static int detectionCount;

    public void handleNodeDeparture(String nodeId) {
        System.err.println("Handling node departure");
        detectionCount++;
        departedNodesHash.put(nodeId, this.getNodeIdFull());
    };

}

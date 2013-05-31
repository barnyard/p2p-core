package com.bt.pi.core.routing;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.RouteMessage;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.routing.Router;
import rice.pastry.standard.StandardRouter;

/**
 * This class wraps a pastry routing strategy that is unfortunately inaccessible, to give us a way of rerouting a
 * RouteMessage to a different destination node ID, recalculating the next hop in the process
 */
public class RoutingMessageRedirector {
    private static final Log LOG = LogFactory.getLog(RoutingMessageRedirector.class);
    private PastryNode pastryNode;
    private RouterWrapper routerWrapper;

    public RoutingMessageRedirector(PastryNode aPastryNode) {
        this.pastryNode = aPastryNode;
        routerWrapper = new RouterWrapper();
    }

    class RouterWrapper extends StandardRouter {
        private Router wrappedRouter;

        public RouterWrapper() {
            super(pastryNode, null);
            this.wrappedRouter = pastryNode.getRouter();
        }

        public NodeHandle pick(RouteMessage routeMessage, Id newDestinationId) {
            if (!(newDestinationId instanceof rice.pastry.Id))
                throw new RuntimeException("Unexpected type: " + newDestinationId.getClass());
            Iterator<rice.pastry.NodeHandle> iter = wrappedRouter.getBestRoutingCandidates((rice.pastry.Id) newDestinationId);
            return super.routerStrategy.pickNextHop((rice.pastry.routing.RouteMessage) routeMessage, iter);
        }
    }

    public void reroute(RouteMessage routeMessage, Id newDestinationId) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Rerouting message %s to id %s", routeMessage, newDestinationId.toStringFull()));
        NodeHandle nextHop = routerWrapper.pick(routeMessage, newDestinationId);
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Setting next hop to %s", nextHop));

        routeMessage.setDestinationId(newDestinationId);
        routeMessage.setNextHopHandle(nextHop);
    }
}

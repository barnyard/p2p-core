package com.bt.pi.core.routing;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.routing.RoutingMessageRedirector;

import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;
import rice.pastry.routing.RouteMessage;
import rice.pastry.Id;
import rice.pastry.NodeHandle;
import rice.pastry.PastryNode;
import rice.pastry.routing.Router;

public class RoutingMessageRedirectorTest {
	private PastryNode pastryNode;
	private Router router;
	private Id id;
	private Logger logger;
	private LogManager logManager;
	private Environment environment;
	private RouteMessage routeMessage;
	private RoutingMessageRedirector routingMessageRedirector;
	private NodeHandle nodeHandle;
	
	@Before
	public void before() {
		id = Id.build("moo");
		
		nodeHandle = mock(NodeHandle.class);
		
		List<NodeHandle> routingCandidateList = new ArrayList<NodeHandle>();
		routingCandidateList.add(nodeHandle);
		router = mock(Router.class);
		when(router.getBestRoutingCandidates(id)).thenReturn(routingCandidateList.iterator());
		
		logger = mock(Logger.class);
        logManager = mock(LogManager.class);
        when(logManager.getLogger(isA(Class.class), isA(String.class))).thenReturn(logger);

        environment = mock(Environment.class);
        when(environment.getLogManager()).thenReturn(logManager);
		
		pastryNode = mock(PastryNode.class);
        when(pastryNode.getEnvironment()).thenReturn(environment);
        when(pastryNode.getRouter()).thenReturn(router);
        
        routeMessage = mock(RouteMessage.class);
        
		this.routingMessageRedirector = new RoutingMessageRedirector(pastryNode);
	}
	
	@Test
	public void shouldUpdateRouteMessageOnRedirect() {
		// act
		routingMessageRedirector.reroute(routeMessage, id);
		
		// assert
		verify(routeMessage).setDestinationId(id);
		verify(routeMessage).setNextHopHandle(nodeHandle);
	}
}

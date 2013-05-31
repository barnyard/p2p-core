package com.bt.pi.core.past.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.bt.pi.core.past.content.KoalaContentHandleBase;

import rice.environment.Environment;
import rice.environment.time.TimeSource;
import rice.p2p.commonapi.NodeHandle;
import rice.pastry.Id;

public class GenericContentHandleTest {

	private long time = 123456789L;

	@Test
	public void constructorTest() {
		NodeHandle nh = mock(NodeHandle.class);

		TimeSource timeSource = mock(TimeSource.class);
		when(timeSource.currentTimeMillis()).thenReturn(time);

		Environment environment = mock(Environment.class);
		when(environment.getTimeSource()).thenReturn(timeSource);
		KoalaContentHandleBase genericContentHandle = new KoalaContentHandleBase(Id.build("YAY"), nh, -1, environment);

		assertEquals(Id.build("YAY"), genericContentHandle.getId());
		assertEquals(nh, genericContentHandle.getNodeHandle());
		assertEquals(-1, genericContentHandle.getVersion(), 0.0);
		assertEquals(time, genericContentHandle.getTimeStamp());
	}
}

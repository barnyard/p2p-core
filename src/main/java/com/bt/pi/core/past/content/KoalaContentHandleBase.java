//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.past.content;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.gc.GCPastContentHandle;

public class KoalaContentHandleBase implements GCPastContentHandle {

	private static final int ONE_WEEK_IN_MILLIS = 7 * 24 * 3600 * 1000;
	private static final long serialVersionUID = -374740649163600460L;
	private Id id;
	private NodeHandle nodeHandle;
	private long timeStamp;
	private long version;

	public KoalaContentHandleBase(Id anId, NodeHandle aNodeHandle, long versionNum, Environment env) {
		id = anId;
		nodeHandle = aNodeHandle;
		timeStamp = env.getTimeSource().currentTimeMillis();
		version = versionNum;
	}

	public Id getId() {
		return id;
	}

	public NodeHandle getNodeHandle() {
		return nodeHandle;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(super.toString()).append(",id=" + getId()).append(",version=" + getVersion()).append(",nodeHandle=" + getNodeHandle()).append(
				",timeStamp=" + getTimeStamp()).toString();
	}

	@Override
	public long getExpiration() {
		return ONE_WEEK_IN_MILLIS + timeStamp;
	}

	@Override
	public long getVersion() {
		return version;
	}

}

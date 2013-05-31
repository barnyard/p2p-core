//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.cli.commands;

import com.bt.pi.core.node.KoalaNode;

public abstract class KoalaNodeCommand extends CommandBase {
	private KoalaNode koalaNode;
	
	public KoalaNodeCommand() {
	}
	
	public KoalaNode getKoalaNode() {
		return koalaNode;
	}
	
	public void setKoalaNode(KoalaNode aKoalaNode) {
		this.koalaNode = aKoalaNode;
	}
}

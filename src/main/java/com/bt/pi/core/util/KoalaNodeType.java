package com.bt.pi.core.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum KoalaNodeType {

	GENERIC(0), CLOUDCONTROLLER(1), NODECONTROLLER(2);

	private static final Map<Integer, KoalaNodeType> lookup = new HashMap<Integer, KoalaNodeType>();

	static {
		for (KoalaNodeType ktype : EnumSet.allOf(KoalaNodeType.class))
			lookup.put(ktype.getCode(), ktype);
	}

	private int code;

	private KoalaNodeType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static KoalaNodeType get(int code) {
		return lookup.get(code);
	}
}

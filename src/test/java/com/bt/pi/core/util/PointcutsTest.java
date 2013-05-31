package com.bt.pi.core.util;

import org.junit.Test;

import com.bt.pi.core.util.Pointcuts;

public class PointcutsTest {
    @Test
    public void codeCoverage() {
        Pointcuts pc = new Pointcuts();
        pc.anyPublicOperation();
        pc.inboundLogging();
        pc.inManagementLayer();
        pc.inRice();
        pc.unused();
        pc.outboundLogging();
    }
}

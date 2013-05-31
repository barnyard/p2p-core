package com.bt.pi.core.bootstrap;

import java.net.InetSocketAddress;
import java.util.List;

public interface NodeBootstrapStrategy {
    List<InetSocketAddress> getBootstrapList();
}

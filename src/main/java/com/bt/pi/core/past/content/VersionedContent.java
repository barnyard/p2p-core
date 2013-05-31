package com.bt.pi.core.past.content;

public interface VersionedContent {
    long getVersion();

    void incrementVersion();

    void setVersion(long version);
}

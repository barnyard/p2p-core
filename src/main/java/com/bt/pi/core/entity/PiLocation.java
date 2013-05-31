package com.bt.pi.core.entity;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.bt.pi.core.scope.NodeScope;
import com.bt.pi.core.scope.Scoped;

public class PiLocation implements Scoped, Locatable {
    private NodeScope nodeScope;
    private String url;

    public PiLocation(String aUrl, NodeScope aNodeScope) {
        this.url = aUrl;
        this.nodeScope = aNodeScope;
    }

    @Override
    public NodeScope getNodeScope() {
        return this.nodeScope;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(url).append(nodeScope).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nodeScope == null) ? 0 : nodeScope.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PiLocation other = (PiLocation) obj;
        if (nodeScope == null) {
            if (other.nodeScope != null)
                return false;
        } else if (!nodeScope.equals(other.nodeScope))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }
}

package com.bt.pi.core.application.resource;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ConsumedUriResourceRegistry extends AbstractConsumedResourceRegistry<URI> {
    public ConsumedUriResourceRegistry() {
        super();
    }

    @Override
    protected String getKeyAsString(URI resourceId) {
        return resourceId.toString();
    }

    public Set<URI> getResourceIdsByScheme(String uriScheme) {
        Set<URI> schemeSet = new HashSet<URI>();
        ConcurrentHashMap<URI, ConsumedResourceState<URI>> map = getResourceMap();
        for (URI uri : map.keySet()) {
            if (uri.getScheme().equals(uriScheme))
                schemeSet.add(uri);
        }
        return schemeSet;
    }
}

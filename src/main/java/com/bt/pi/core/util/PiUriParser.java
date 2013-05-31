package com.bt.pi.core.util;

import java.util.HashMap;
import java.util.Map;

public class PiUriParser {
    private static final String COLON = ":";
    private static final String SEMICOLON = ";";
    // private static final Log LOG = LogFactory.getLog(PiUriParser.class);
    private static final String EQUALS = "=";
    private String uri;
    private String scheme;
    private Map<String, String> params;
    private String resourceId;

    public PiUriParser(String aUri) {
        this.uri = aUri;
        parse();
    }

    private void parse() {
        int schemeIndex = uri.indexOf(COLON);
        if (schemeIndex < 1)
            throw new PiUriParseException(String.format("No scheme in uri %s", uri));

        this.scheme = uri.substring(0, schemeIndex);
        this.params = new HashMap<String, String>();

        int firstParamIndex = uri.indexOf(SEMICOLON);
        if (firstParamIndex > 0) {
            this.resourceId = uri.substring(schemeIndex + 1, firstParamIndex);
        } else {
            this.resourceId = uri.substring(schemeIndex + 1);
        }

        String[] paramParts = uri.split(SEMICOLON);
        if (paramParts.length > 1) {
            for (int i = 1; i < paramParts.length; i++) {
                String currentParamKeyValPair = paramParts[i];
                if (currentParamKeyValPair.indexOf(EQUALS) < 1 || currentParamKeyValPair.indexOf(EQUALS) != currentParamKeyValPair.lastIndexOf(EQUALS))
                    throw new PiUriParseException(String.format("Invalid parameter: %s", currentParamKeyValPair));
                String[] keyVal = currentParamKeyValPair.split(EQUALS);
                if (keyVal.length > 1)
                    params.put(keyVal[0], keyVal[1]);
                else
                    params.put(keyVal[0], "");
            }
        }
    }

    public String getScheme() {
        return scheme;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getParameter(String name) {
        return params.get(name);
    }

    public Map<String, String> getParameterMap() {
        return params;
    }
}

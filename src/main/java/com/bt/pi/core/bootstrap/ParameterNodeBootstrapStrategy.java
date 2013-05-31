package com.bt.pi.core.bootstrap;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.environment.params.Parameters;

import com.bt.pi.core.exception.KoalaNodeInitializationException;

public class ParameterNodeBootstrapStrategy implements NodeBootstrapStrategy {
    public static final String KOALA_PREFERRED_BOOTSTRAPS_PARAM = "koala_preferred_bootstraps";
    private static final String[] DEFAULT_BOOTSTRAPS = { "127.0.0.1:4524" };
    private static final Log LOG = LogFactory.getLog(ParameterNodeBootstrapStrategy.class);

    private Parameters parameters;

    public ParameterNodeBootstrapStrategy() {
    }

    public ParameterNodeBootstrapStrategy(Parameters params) {
        parameters = params;
    }

    public void setParameters(Parameters params) {
        parameters = params;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public List<InetSocketAddress> getBootstrapList() {
        LOG.debug("getBootstrapList()");

        if (StringUtils.isBlank(parameters.getString(KOALA_PREFERRED_BOOTSTRAPS_PARAM))) {
            parameters.setStringArray(KOALA_PREFERRED_BOOTSTRAPS_PARAM, DEFAULT_BOOTSTRAPS);
        }

        List<InetSocketAddress> bootstrapList = new ArrayList<InetSocketAddress>();
        try {
            bootstrapList.addAll(Arrays.asList(parameters.getInetSocketAddressArray(KOALA_PREFERRED_BOOTSTRAPS_PARAM)));
            LOG.debug("Bootstrap list:" + bootstrapList);
        } catch (UnknownHostException e) {
            throw new KoalaNodeInitializationException(String.format("Error initializing bootstrap addresses %s", parameters.getString(KOALA_PREFERRED_BOOTSTRAPS_PARAM)), e);
        }
        Collections.shuffle(bootstrapList);
        return bootstrapList;
    }
}

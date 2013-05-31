package com.bt.pi.core.conf;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;

public class PropertiesConfigurationLoader {

    private static final Log LOG = LogFactory.getLog(PropertiesConfigurationLoader.class);
    private static final String UNABLE_TO_LOAD_CONFIGURATION_S = "unable to load configuration %s";

    public PropertiesConfigurationLoader() {

    }

    public PropertiesConfiguration loadConfig(Resource resource, int refreshInterval, ConfigurationListener configurationListener) {
        URL url = null;
        try {
            url = resource.getURL();
            LOG.info(String.format("Loading config from %s", url));
            PropertiesConfiguration configuration = new PropertiesConfiguration(url);
            FileChangedReloadingStrategy strat = new FileChangedReloadingStrategy();
            strat.setRefreshDelay(refreshInterval);
            LOG.debug(String.format("setRefreshInterval(%d)", refreshInterval));
            configuration.setReloadingStrategy(strat);
            configuration.addConfigurationListener(configurationListener);
            return configuration;
        } catch (IOException e) {
            LOG.error(String.format(UNABLE_TO_LOAD_CONFIGURATION_S, resource.getFilename()), e);
        } catch (ConfigurationException e) {
            LOG.error(String.format(UNABLE_TO_LOAD_CONFIGURATION_S, resource.getFilename()), e);
        }

        return null;
    }

}

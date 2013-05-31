//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.core.cli.KoalaCli;
import com.bt.pi.core.cli.commands.CommandParser;

public class Main {
    private static Log LOG = LogFactory.getLog(Main.class);

    private static final String APPLICATION_CONTEXT_OPTION_FLAG = "x";
    private KoalaCli koalaCli;
    private CloudPlatform platform;
    private AbstractApplicationContext applicationContext;
    private CommandLine line;

    public Main() {
        koalaCli = null;
        applicationContext = null;
        platform = null;
    }

    public static void main(String args[]) throws MalformedURLException, IOException {
        Main main = new Main();
        main.init(args, true);
    }

    public AbstractApplicationContext init(String args[], boolean shouldStart) {
        Thread.setDefaultUncaughtExceptionHandler(new PiUncaughtExceptionHandler());

        Options options = new Options();
        options.addOption("c", "cli", false, "Start command line tool");
        options.addOption("a", "address-pattern", true, "Address pattern to bind to");
        options.addOption("p", "port", true, "Port to use for the node");
        options.addOption("b", "bootstrap", true, "Bootstrap addresses to use");
        options.addOption("s", "single-machine", true, "Configure pi to run multiple nodes on the same machine");
        options.addOption("h", "help", false, "Show available options");
        options.addOption("g", "gui", false, "Start gui tool");
        options.addOption(APPLICATION_CONTEXT_OPTION_FLAG, "applicationConteXt", true, "The application context files to use");

        CommandLineParser parser = new GnuParser();
        try {
            line = parser.parse(options, args);
        } catch (ParseException exp) {
            LOG.error("Parsing failed.  Reason: " + exp.getMessage());
            return null;
        }

        if (line.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("koalap2p", options);
            return null;
        }

        ContextDiscoverer contextDiscoverer = new ContextDiscoverer();
        String applicationContextLocation = contextDiscoverer.findPiContexts();

        if (line.hasOption(APPLICATION_CONTEXT_OPTION_FLAG)) {
            applicationContextLocation = applicationContextLocation + "," + line.getOptionValue(APPLICATION_CONTEXT_OPTION_FLAG);
        }

        loadApplicationContext(applicationContextLocation);
        applicationContext.registerShutdownHook();
        platform = (CloudPlatform) applicationContext.getBean("cloudPlatform");

        LOG.debug("context bean count: " + applicationContext.getBeanDefinitionCount());
        LOG.debug("context bean count: " + Arrays.toString(applicationContext.getBeanDefinitionNames()));

        if (shouldStart) {
            // starting p2p platform
            try {
                platform.start(line);
            } catch (Throwable t) {
                LOG.fatal("error starting cloud platform", t);
                System.exit(1);
            }
        }

        if (line.hasOption("c")) {
            koalaCli = new KoalaCli(platform.getKoalaNode());
            koalaCli.setCommandParser((CommandParser) applicationContext.getBean("commandParser"));
            koalaCli.start();
            System.exit(0);
        }

        return applicationContext;
    }

    public void start() {
        platform.start(line);
    }

    public CloudPlatform getCloudPlatform() {
        return platform;
    }

    public AbstractApplicationContext loadApplicationContext(String contextPath) {
        try {
            LOG.debug("Loading application contexts from: " + contextPath);
            String[] paths = contextPath.split(",");
            applicationContext = new ClassPathXmlApplicationContext(paths);
            return applicationContext;
        } catch (Throwable t) {
            LOG.error("Unable to load application context file:", t);
            throw new RuntimeException("Unable to load application context file", t);
        }
    }
}

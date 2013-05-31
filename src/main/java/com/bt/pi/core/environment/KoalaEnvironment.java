package com.bt.pi.core.environment;

import java.io.File;
import java.net.InetAddress;
import java.util.TimeZone;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.stereotype.Component;

import rice.environment.Environment;
import rice.environment.exception.ExceptionStrategy;
import rice.environment.logging.LogManager;
import rice.environment.params.Parameters;
import rice.environment.processing.Processor;
import rice.environment.random.RandomSource;
import rice.environment.time.TimeSource;

import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.pastry_override.PiSelectorManager;

@Component("koalaEnvironment")
public class KoalaEnvironment {
    private static final Log LOG = LogFactory.getLog(KoalaEnvironment.class);
    private static final long LOG4J_WATCHER_INTERVAL = 1000 * 60;
    private Environment environment;
    private Parameters parameters;
    private LogManager logManager;
    private ExceptionStrategy exceptionStrategy;

    @Resource
    private PiSelectorManager selectorManager;
    private File log4jFile;

    public KoalaEnvironment() {
        environment = null;
        parameters = null;
        logManager = null;
        exceptionStrategy = null;
        selectorManager = null;
        log4jFile = null;
    }

    // This is so gross and nasty. Need to fix this later
    public void initPastryEnvironment(InetAddress inetAddress, int port) {
        LOG.debug(String.format("initPastryEnvironment(%s, %d)", inetAddress.getHostAddress(), port));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        TimeSource timeSource = Environment.generateDefaultTimeSource();
        RandomSource randomSource = Environment.generateDefaultRandomSource(parameters, logManager);
        Processor proc = Environment.generateDefaultProcessor();

        selectorManager.setInetAddress(inetAddress);
        selectorManager.setPort(port);
        selectorManager.doPostInitializationTasks();
        environment = new Environment(selectorManager, proc, randomSource, timeSource, logManager, parameters, exceptionStrategy);

        // setup Log4j.xml watcher
        DOMConfigurator.configureAndWatch(log4jFile.getAbsolutePath(), LOG4J_WATCHER_INTERVAL);

    }

    @Resource(name = "log4jFile")
    public void setLog4jFile(File aLog4jFile) {
        this.log4jFile = aLog4jFile;
    }

    public Environment getPastryEnvironment() {
        return environment;
    }

    @Resource
    public void setParameters(Parameters params) {
        parameters = params;
    }

    public Parameters getParameters() {
        return parameters;
    }

    @Resource
    public void setExceptionStrategy(ExceptionStrategy strategy) {
        exceptionStrategy = strategy;
    }

    @Resource
    public void setLogManager(Log4JLogManager aLogManager) {
        logManager = aLogManager;
    }

}
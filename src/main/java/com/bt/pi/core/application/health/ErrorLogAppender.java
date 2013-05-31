package com.bt.pi.core.application.health;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.util.MDCHelper;
import com.bt.pi.core.util.collections.ConcurrentSortedBoundQueue;

@Component
public class ErrorLogAppender extends AppenderSkeleton implements ApplicationListener<NodeStartedEvent> {
    private static final String NAME = "Error Log Appender";
    private static final Log LOG = LogFactory.getLog(ErrorLogAppender.class);

    private static final String REDIRECTING_FROM_STD = "^Redirecting from std.*";
    private static final List<String> ALWAYS_FILTER_LIST = Arrays.asList(REDIRECTING_FROM_STD);

    private static final String REPORT_DELAYS_SECONDS = "300";
    private static final String DEFAULT_MAX_QUEUE_SIZE = "100";

    private PatternLayout patternLayout;
    private int maxQueueSize;
    private Level appenderLevel;
    private int sizePerLogMessage;
    private long publishIntervalSeconds;
    private ConcurrentSortedBoundQueue<LogMessageEntity> logQueue;
    private AtomicBoolean nodeStarted;
    private List<String> filterList;

    @Resource
    private ReportingApplication reportingApplication;
    private ScheduledExecutorService scheduledExecutorService;

    public ErrorLogAppender() {
        setName(NAME);
        publishIntervalSeconds = Integer.parseInt(REPORT_DELAYS_SECONDS);
        patternLayout = new PatternLayout();
        logQueue = new ConcurrentSortedBoundQueue<LogMessageEntity>(Integer.parseInt(DEFAULT_MAX_QUEUE_SIZE));
        nodeStarted = new AtomicBoolean(false);
        reportingApplication = null;
        scheduledExecutorService = null;
        filterList = null;
    }

    @Resource
    public void setScheduledExecutorService(ScheduledExecutorService aScheduledExecutorService) {
        scheduledExecutorService = aScheduledExecutorService;
    }

    @Resource(name = "errorLogAppenderFilterList")
    public void setFilterList(List<String> aFilterList) {
        filterList = aFilterList;
    }

    @Property(key = "error.log.publishintervalsize", defaultValue = REPORT_DELAYS_SECONDS)
    public void setPublishIntervalSeconds(int aPublishIntervalSeconds) {
        publishIntervalSeconds = aPublishIntervalSeconds;
    }

    @Property(key = "error.log.appender.pattern", defaultValue = "%C{1}")
    public void setErrorLogAppenderPattern(String value) {
        patternLayout.setConversionPattern(value);
    }

    @Property(key = "error.log.appender.max.queue.size", defaultValue = DEFAULT_MAX_QUEUE_SIZE)
    public void setErrorLogAppenderMaxQueueSize(int value) {
        if (maxQueueSize != value) {
            maxQueueSize = value;
            logQueue.setSize(maxQueueSize);
        }
    }

    @Property(key = "error.log.appender.level", defaultValue = "ERROR")
    public void setErrorLogAppenderLevel(String value) {
        appenderLevel = Level.toLevel(value);
    }

    @Property(key = "error.log.appender.max.characters.per.log.message", defaultValue = "250")
    public void setMaxCharactersPerLogMessage(int value) {
        sizePerLogMessage = value;
    }

    @PostConstruct
    public void addToAllLoggersAndScheduleReportingOfLogMessages() {
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                addToAllLoggers();
                sendLogMessagesToSuperNodes();
            }
        }, 0, publishIntervalSeconds, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private void addToAllLoggers() {
        addToLogger(LogManager.getRootLogger());
        Enumeration<Logger> currentLoggers = LogManager.getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            addToLogger(currentLoggers.nextElement());
        }
    }

    protected void addToLogger(Logger logger) {
        if (logger.getAppender(NAME) == null)
            logger.addAppender(this);
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (loggingEvent.getLevel().isGreaterOrEqual(appenderLevel)) {
            if (!matchesFilterList(loggingEvent.getRenderedMessage()))
                logQueue.add(new LogMessageEntity(loggingEvent.getTimeStamp(), StringUtils.abbreviate(loggingEvent.getRenderedMessage(), sizePerLogMessage), patternLayout.format(loggingEvent), (String) loggingEvent
                        .getMDC(MDCHelper.MDC_TRANSACTION_ID), getNodeId()));
        }
    }

    private boolean matchesFilterList(String renderedMessage) {
        if (renderedMessage == null)
            return true;

        for (String filterItem : ALWAYS_FILTER_LIST)
            if (renderedMessage.matches(filterItem))
                return true;

        if (filterList == null || filterList.isEmpty())
            return false;

        for (String filterItem : filterList)
            if (renderedMessage.matches(filterItem))
                return true;

        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public void sendLogMessagesToSuperNodes() {
        if (nodeStarted.get()) {
            LOG.debug("Sending log messages to super nodes...");
            Collection<LogMessageEntity> messagesToSend = new ArrayList<LogMessageEntity>();
            Iterator<LogMessageEntity> iterator = logQueue.iterator();
            while (iterator.hasNext()) {
                LogMessageEntity logMessageEntity = iterator.next();
                messagesToSend.add(logMessageEntity);
            }

            if (messagesToSend.isEmpty())
                LOG.debug("No log messages to send to super nodes");
            else {
                LogMessageEntityCollection logMessages = new LogMessageEntityCollection();
                logMessages.setEntities(messagesToSend);
                reportingApplication.sendReportingUpdateToASuperNode(logMessages);
            }
        } else
            LOG.debug("Node is not started yet, not sending any messages to super nodes");
    }

    private String getNodeId() {
        String nodeId = null;

        try {
            nodeId = String.format("%s / %s", reportingApplication.getNodeIdFull(), InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            System.err.println("Unable to retrieve hostname: " + e.getMessage());
            nodeId = reportingApplication.getNodeIdFull();
        } catch (NullPointerException npe) {
            System.err.println("Unable to retrieve NodeId");
            try {
                nodeId = String.format("%s ", InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        return nodeId;
    }

    // for asserting in tests
    SortedSet<LogMessageEntity> getLogQueue() {
        return logQueue;
    }

    @Override
    public void onApplicationEvent(NodeStartedEvent event) {
        LOG.debug(String.format("onApplicationEvent(%s)", event));
        nodeStarted.set(true);
    }
}

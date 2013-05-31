package com.bt.pi.core.application.health;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.reporter.ReportingApplication;
import com.bt.pi.core.entity.PiEntityCollection;
import com.bt.pi.core.node.NodeStartedEvent;
import com.bt.pi.core.util.MDCHelper;

@RunWith(MockitoJUnitRunner.class)
public class ErrorLogAppenderTest {
    private String nodeId = "nodeId";
    private long logTimestamp = 101L;
    private String logMessage = "this is a test";
    private LoggingEvent loggingEvent;
    private List<String> filterList;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;
    @Mock
    private ReportingApplication reportingApplication;

    @InjectMocks
    private ErrorLogAppender errorLogAppender = new ErrorLogAppender();

    @Before
    public void setup() {
        filterList = new ArrayList<String>();

        when(reportingApplication.getNodeIdFull()).thenReturn(nodeId);

        errorLogAppender.setErrorLogAppenderLevel("ERROR");
        errorLogAppender.setErrorLogAppenderMaxQueueSize(2);
        errorLogAppender.setMaxCharactersPerLogMessage(20);
        errorLogAppender.setFilterList(filterList);
        errorLogAppender.setErrorLogAppenderPattern("%C{1}");
    }

    @Test
    public void shouldSetNameOnAppender() throws Exception {
        // act
        String name = errorLogAppender.getName();

        // assert
        assertThat(name, equalTo("Error Log Appender"));
    }

    @Test
    public void shouldReturnFalseInRequiresLayout() {
        assertFalse(errorLogAppender.requiresLayout());
    }

    @Test
    public void shouldAddItselfToRootLogger() throws Exception {
        // setup
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), isA(Long.class), isA(Long.class), isA(TimeUnit.class));

        // act
        errorLogAppender.addToAllLoggersAndScheduleReportingOfLogMessages();

        // assert
        assertThat(allLoggersContainErrorLogAppender(), is(true));
    }

    @Test
    public void shouldNotAddToLoggerIfAlreadyAdded() throws Exception {
        // setup
        Logger testLogger = mock(Logger.class);
        when(testLogger.getAppender("Error Log Appender")).thenReturn(errorLogAppender);

        // act
        errorLogAppender.addToLogger(testLogger);

        // assert
        verify(testLogger, never()).addAppender(isA(Appender.class));
    }

    @Test
    public void shouldLogErrorMessages() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(getClass().getName(), Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);

        // assert
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(true));
    }

    @Test
    public void shouldTruncateErrorMessages() throws Exception {
        // setup
        errorLogAppender.setMaxCharactersPerLogMessage(10);
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);

        // assert
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage.substring(0, 7) + "...", null);
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(true));
    }

    @Test
    public void shouldLogFatalMessages() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.FATAL, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);

        // assert
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(true));
    }

    @Test
    public void shouldNotLogWarnMessages() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.WARN, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);

        // assert
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(false));
    }

    @Test
    public void shouldNotLogAnyMessagesIfAppenderIsSetToOff() throws Exception {
        // setup
        errorLogAppender.setErrorLogAppenderLevel("OFF");
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.FATAL, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);

        // assert
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(false));
    }

    @Test
    public void shouldOnlyStoreLastNMessages() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage, null);
        LoggingEvent loggingEvent2 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp + 100000, Level.ERROR, logMessage, null);
        LoggingEvent loggingEvent3 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp + 200000, Level.ERROR, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);
        errorLogAppender.append(loggingEvent2);
        errorLogAppender.append(loggingEvent3);

        // assert
        assertThat(errorLogAppender.getLogQueue().size(), equalTo(2));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(false));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent2)), is(true));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent3)), is(true));
    }

    @Test
    public void shouldStoreOnlyOneCopyOfMessageWithSameTimestampAndLogMessage() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage, null);
        LoggingEvent loggingEvent2 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.FATAL, logMessage, null);

        // act
        errorLogAppender.append(loggingEvent);
        errorLogAppender.append(loggingEvent2);

        // assert
        assertThat(errorLogAppender.getLogQueue().size(), equalTo(1));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)) || errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent2)), is(true));
    }

    @Test
    public void shouldStoreDifferentCopiesOfMessageWithSameTimestampAndDifferentLogMessage() throws Exception {
        // setup
        loggingEvent = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage, null);
        LoggingEvent loggingEvent2 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, logMessage + "2", null);

        // act
        errorLogAppender.append(loggingEvent);
        errorLogAppender.append(loggingEvent2);

        // assert
        assertThat(errorLogAppender.getLogQueue().size(), equalTo(2));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent)), is(true));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent2)), is(true));
    }

    @Test
    public void testThatScheduledMethodPublishesLogsInQueue() throws Exception {
        // setup
        final LogMessageEntity logMessage1 = new LogMessageEntity(1, "l1", "c1", "t1", "nodeId");
        final LogMessageEntity logMessage2 = new LogMessageEntity(2, "l2", "c2", "t2", "nodeId");
        final Collection<LogMessageEntity> logMessages = Arrays.asList(logMessage1, logMessage2);
        errorLogAppender.getLogQueue().addAll(logMessages);
        errorLogAppender.onApplicationEvent(new NodeStartedEvent(this));

        // act
        errorLogAppender.sendLogMessagesToSuperNodes();

        // assert
        verify(reportingApplication).sendReportingUpdateToASuperNode(argThat(new ArgumentMatcher<PiEntityCollection<LogMessageEntity>>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                Collection<? extends Object> entities = ((PiEntityCollection<LogMessageEntity>) argument).getEntities();
                assertThat(entities.size(), equalTo(logMessages.size()));
                assertThat(entities.contains(logMessage1), is(true));
                assertThat(entities.contains(logMessage2), is(true));

                return true;
            }
        }));
    }

    @Test
    public void testThatScheduledMethodDoesNotPublishUntilNodeIsStarted() throws Exception {
        // setup
        final LogMessageEntity logMessage1 = new LogMessageEntity(1, "l1", "c1", "t1", "nodeId");
        final Collection<LogMessageEntity> logMessages = Arrays.asList(logMessage1);
        errorLogAppender.getLogQueue().addAll(logMessages);

        // act
        errorLogAppender.sendLogMessagesToSuperNodes();

        // assert
        verify(reportingApplication, never()).publishToReportingTopic(isA(PiEntityCollection.class));
    }

    @Test
    public void testThatLogMessagesThatHaveBeenRedirectedFromStdOutOrStdErrDontGetLogged() throws Exception {
        // setup
        LoggingEvent loggingEvent0 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "Redirecting from stdout: log", null);
        LoggingEvent loggingEvent1 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "test123", null);
        LoggingEvent loggingEvent2 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "123test", null);
        LoggingEvent loggingEvent3 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "Redirecting from stderr: try it now", null);

        // act
        errorLogAppender.append(loggingEvent0);
        errorLogAppender.append(loggingEvent1);
        errorLogAppender.append(loggingEvent2);
        errorLogAppender.append(loggingEvent3);

        // assert
        assertThat(errorLogAppender.getLogQueue().size(), equalTo(2));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent1)), is(true));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent2)), is(true));
    }

    @Test
    public void testThatLogMessagesMatchingEntriesInTheFilterListDontGetLogged() throws Exception {
        // setup
        filterList.add("^t.*");
        filterList.add("[0-9]+.*");

        LoggingEvent loggingEvent0 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "log", null);
        LoggingEvent loggingEvent1 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "test123", null);
        LoggingEvent loggingEvent2 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "123test", null);
        LoggingEvent loggingEvent3 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "try it now", null);
        LoggingEvent loggingEvent4 = new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "abcd", null);

        // act
        errorLogAppender.append(loggingEvent0);
        errorLogAppender.append(loggingEvent1);
        errorLogAppender.append(loggingEvent2);
        errorLogAppender.append(loggingEvent3);
        errorLogAppender.append(loggingEvent4);

        // assert
        assertThat(errorLogAppender.getLogQueue().size(), equalTo(2));
        System.err.println(errorLogAppender.getLogQueue());
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent0)), is(true));
        assertThat(errorLogAppender.getLogQueue().contains(convertToLogMessageEntity(loggingEvent4)), is(true));
    }

    @Test
    public void shouldSpinOffSchedulerThreadOnPostConstruct() throws Exception {
        // act
        errorLogAppender.addToAllLoggersAndScheduleReportingOfLogMessages();

        // assert
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0l), eq(300l), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldAddLoggingMessagesWithNotNullNodeIdToQueue() {
        // setup
        errorLogAppender.getLogQueue().clear();

        // act
        errorLogAppender.append(new LoggingEvent(null, Logger.getLogger(getClass()), logTimestamp, Level.ERROR, "log", null));
        LogMessageEntity entity = errorLogAppender.getLogQueue().first();
        // assert
        System.err.println("NODE ID: " + entity.getNodeId());
        assertNotNull(entity.getNodeId());
        assertFalse(StringUtils.isEmpty(entity.getNodeId()));

    }

    @SuppressWarnings("unchecked")
    private boolean allLoggersContainErrorLogAppender() {
        Enumeration<Logger> currentLoggers = LogManager.getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            Logger logger = currentLoggers.nextElement();
            if (!loggerContainsErrorLogAppender(logger))
                return false;
        }
        return loggerContainsErrorLogAppender(LogManager.getRootLogger());
    }

    @SuppressWarnings("unchecked")
    private boolean loggerContainsErrorLogAppender(Logger logger) {
        Enumeration<Appender> allAppenders = (Enumeration<Appender>) logger.getAllAppenders();
        while (allAppenders.hasMoreElements()) {
            Appender nextAppender = allAppenders.nextElement();
            if (nextAppender == errorLogAppender) {
                return true;
            }
        }
        return false;
    }

    private LogMessageEntity convertToLogMessageEntity(LoggingEvent loggingEvent) throws UnknownHostException {
        return new LogMessageEntity(loggingEvent.getTimeStamp(), loggingEvent.getRenderedMessage(), new PatternLayout("%C{1}").format(loggingEvent), (String) loggingEvent.getMDC(MDCHelper.MDC_TRANSACTION_ID), String.format("%s / %s", nodeId,
                InetAddress.getLocalHost().getHostName()));
    }
}

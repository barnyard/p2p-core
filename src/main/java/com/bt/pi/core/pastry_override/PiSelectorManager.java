package com.bt.pi.core.pastry_override;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.params.Parameters;
import rice.environment.random.RandomSource;
import rice.selector.LoopObserver;
import rice.selector.SelectionKeyHandler;
import rice.selector.SelectorManager;
import rice.selector.TimerTask;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.logging.Log4JLogManager;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.core.util.TimeBasedLogger;
import com.bt.pi.core.util.TimeBasedLoggerConfiguration;

@Component
public class PiSelectorManager extends SelectorManager {
    private static final String EIGHT_HUNDRED = "800";
    private static final String THREE_HUNDRED = "300";
    private static final String PASTRY_PORT = "pastry_port";

    private static Log LOG = LogFactory.getLog(PiSelectorManager.class);

    private AtomicInteger maxInvocationTimeMillis;
    private AtomicInteger maxExecuteTasksTimeMillis;
    private AtomicInteger selectorSleepTimeMillis;
    private ConcurrentLinkedQueue<Runnable> piInvocations;
    private PriorityBlockingQueue<TimerTask> piTimerQueue;
    private CopyOnWriteArrayList<LoopObserver> piLoopObservers;
    private ConcurrentLinkedQueue<SelectionKey> piModifiedKeys;
    private long pilastTime;
    private int port = KoalaNode.DEFAULT_PORT;
    private TimeBasedLogger<Void> doSelectionsLogger;

    private TimeBasedLogger<Void> notifyListenersLogger;

    private TimeBasedLogger<Void> addTaskLogger;
    private TimeBasedLoggerConfiguration<Void> addTaskLoggerConfiguration;
    private TimeBasedLogger<Void> doInvocationsLogger;
    private TimeBasedLogger<Void> executeDueTasksLogger;
    private TimeBasedLogger<Boolean> executeTaskLogger;
    private TimeBasedLoggerConfiguration<Boolean> executeTaskLoggerConfiguration;
    private TimeBasedLogger<Void> runTaskLogger;
    private TimeBasedLoggerConfiguration<Void> runTaskLoggerConfiguration;

    @Resource
    private Parameters parameters;

    private InetAddress inetAddress;

    @Resource
    private LogManager logManager;

    public PiSelectorManager() {

        super(null, Environment.generateDefaultTimeSource(), new Log4JLogManager(), null);
        configureDoInvocationsLogger();
        configureDoSelectionsLogger();
        configureNotifyListenersLogger();
        configureAddTaskLogger();
        configureExecuteDueTasksLogger();
        configureExecuteTaskLogger();
        configureRunTaskLogger();
        resetFields();

    }

    private void configureRunTaskLogger() {
        runTaskLoggerConfiguration = new TimeBasedLoggerConfiguration<Void>(true, LOG, 200, TimeBasedLoggerConfiguration.DEFAULT_TIME_BETWEEN_LOGS);

        runTaskLogger = new TimeBasedLogger<Void>(runTaskLoggerConfiguration);

    }

    private void configureExecuteTaskLogger() {
        executeTaskLoggerConfiguration = new TimeBasedLoggerConfiguration<Boolean>(true, LOG, 200, TimeBasedLoggerConfiguration.DEFAULT_TIME_BETWEEN_LOGS);

        executeTaskLogger = new TimeBasedLogger<Boolean>(executeTaskLoggerConfiguration);

    }

    private void configureDoInvocationsLogger() {
        TimeBasedLoggerConfiguration<Void> doInvocationsLoggerConfiguration = new TimeBasedLoggerConfiguration<Void>(true, new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                long runTimeWindow = System.currentTimeMillis() + maxInvocationTimeMillis.get();

                while (System.currentTimeMillis() < runTimeWindow && (!piInvocations.isEmpty() || !piModifiedKeys.isEmpty())) {
                    Runnable run = piInvocations.poll();
                    if (run != null) {
                        try {
                            runTask(run);

                        } catch (RuntimeException e) {
                            // maybe we can look at reading or something.
                            LOG.error("Error executing invocation: " + run, e);
                        }
                    }
                    SelectionKey key = piModifiedKeys.poll();
                    if (key != null && key.isValid() && (key.attachment() != null)) {
                        ((SelectionKeyHandler) key.attachment()).modifyKey(key);
                    }
                }

                return null;
            }
        }, getClass().getSimpleName() + ".doSelections()", LOG, 400, TimeBasedLoggerConfiguration.DEFAULT_TIME_BETWEEN_LOGS);
        doInvocationsLogger = new TimeBasedLogger<Void>(doInvocationsLoggerConfiguration);
    }

    private void configureDoSelectionsLogger() {
        TimeBasedLoggerConfiguration<Void> doSelectionsLoggerConfiguration = new TimeBasedLoggerConfiguration<Void>(true, new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                callDoSelectionsInSelectorManager();
                return null;
            }
        }, getClass().getSimpleName() + ".doSelections()", LOG);
        doSelectionsLogger = new TimeBasedLogger<Void>(doSelectionsLoggerConfiguration);
    }

    private void configureNotifyListenersLogger() {
        TimeBasedLoggerConfiguration<Void> notifyListenersLoggerConfiguration = new TimeBasedLoggerConfiguration<Void>(true, new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                long now = timeSource.currentTimeMillis();
                long diff = now - pilastTime;
                // notify observers
                for (LoopObserver lo : piLoopObservers) {
                    if (lo.delayInterest() <= diff) {
                        lo.loopTime((int) diff);
                    }
                }
                pilastTime = now;

                return null;
            }
        }, getClass().getSimpleName() + ".notifyListeners()", LOG);
        notifyListenersLogger = new TimeBasedLogger<Void>(notifyListenersLoggerConfiguration);
    }

    public void configureExecuteDueTasksLogger() {
        TimeBasedLoggerConfiguration<Void> configuration = new TimeBasedLoggerConfiguration<Void>(true, new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                callExecuteDueTasks();
                return null;
            }
        }, getClass().getSimpleName() + ".executeDueTasks()", LOG, 400, TimeBasedLoggerConfiguration.DEFAULT_TIME_BETWEEN_LOGS);
        executeDueTasksLogger = new TimeBasedLogger<Void>(configuration);

    }

    private void configureAddTaskLogger() {
        addTaskLoggerConfiguration = new TimeBasedLoggerConfiguration<Void>();
        addTaskLoggerConfiguration.setActive(true);
        addTaskLoggerConfiguration.setLog(LOG);
        addTaskLogger = new TimeBasedLogger<Void>(addTaskLoggerConfiguration);
    }

    private void resetFields() {
        piInvocations = new ConcurrentLinkedQueue<Runnable>();
        piTimerQueue = new PriorityBlockingQueue<TimerTask>();
        piLoopObservers = new CopyOnWriteArrayList<LoopObserver>();
        piModifiedKeys = new ConcurrentLinkedQueue<SelectionKey>();
        pilastTime = 0;

        maxInvocationTimeMillis = new AtomicInteger(Integer.parseInt(EIGHT_HUNDRED));
        maxExecuteTasksTimeMillis = new AtomicInteger(Integer.parseInt(EIGHT_HUNDRED));
        selectorSleepTimeMillis = new AtomicInteger(Integer.parseInt(THREE_HUNDRED));

    }

    public void doPostInitializationTasks() {
        if (parameters.contains(PASTRY_PORT)) {
            this.port = parameters.getInt(PASTRY_PORT);
        }
        String selectorManagerName = inetAddress.getHostAddress() + "." + port;
        RandomSource randomSource = Environment.generateDefaultRandomSource(parameters, logManager);
        this.instance = selectorManagerName;
        setName(instance);
        this.random = randomSource;
        this.logger = logManager.getLogger(super.getClass(), this.instance);

        if (LOG.isDebugEnabled())
            LOG.debug("Selector ManagerName: " + selectorManagerName + " original priority: " + this.getPriority());
        this.setPriority(Thread.MAX_PRIORITY);
        if (LOG.isDebugEnabled())
            LOG.debug("SelectorManager new priority: " + this.getPriority());

    }

    /*
     * Loop listeners code
     * 
     */

    @Override
    public void notifyLoopListeners() {
        try {
            notifyListenersLogger.call();
        } catch (Exception e) {
            LOG.warn("Exception during notifyLoopListeners", e);
        }
    }

    @Override
    public void addLoopObserver(LoopObserver lo) {
        piLoopObservers.add(lo);
    }

    @Override
    public void removeLoopObserver(LoopObserver lo) {
        piLoopObservers.remove(lo);
    }

    /*
     * Invocation Code
     * 
     */

    /* (non-Javadoc)
     * @see com.bt.pi.core.pastry_override.PiSelectorManager#invoke(java.lang.Runnable)
     */

    @Override
    public void invoke(Runnable d) {
        if (d == null)
            throw new IllegalArgumentException("A null runnable object is not accepted.");
        if (piInvocations == null)
            return;
        piInvocations.add(d);

        wakeup();
    }

    @Override
    public int getNumInvocations() {
        return piInvocations.size();
    }

    @Override
    public void doInvocations() {
        try {
            doInvocationsLogger.call();
        } catch (Exception e) {
            LOG.warn("Error during do Invocations", e);
        }
    }

    public void runTask(final Runnable task) {
        runTaskLoggerConfiguration.setCallableToRun(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                task.run();
                return null;
            }
        });
        runTaskLoggerConfiguration.setLogHeader(getClass().getSimpleName() + ".runTask(" + task + ")");
        try {
            runTaskLogger.call();
        } catch (Exception e) {
            LOG.warn("Exception running task: " + task, e);
        }
    }

    @Override
    protected Runnable getInvocation() {
        return (Runnable) piInvocations.poll();
    }

    /*
     * ExecuteDueTasks code
     * 
     */
    /* (non-Javadoc)
     * @see com.bt.pi.core.pastry_override.PiSelectorManager#executeDueTasks()
     */

    @Override
    public void executeDueTasks() {
        try {
            executeDueTasksLogger.call();
        } catch (Exception e) {
            LOG.warn("Exception during Execute due tasks", e);
        }
    }

    private void callExecuteDueTasks() {
        long now = timeSource.currentTimeMillis();
        ArrayList<TimerTask> executeNow = new ArrayList<TimerTask>();

        // step 1, fetch all due timers

        boolean done = false;
        while (!done) {
            if (!piTimerQueue.isEmpty()) {
                TimerTask next = (TimerTask) piTimerQueue.peek();
                if (next.scheduledExecutionTime() <= now) {
                    executeNow.add(next);
                    TimerTask polledTask = piTimerQueue.poll();
                    if (LOG.isDebugEnabled())
                        LOG.debug("Preparing to run task: " + polledTask);
                } else {
                    done = true;
                }
            } else {
                done = true;
            }
        }

        // step 2, execute them all
        // items to be added back into the queue
        if (executeNow.size() > 100) {
            LOG.warn("# of due tasks:  " + executeNow.size());
        }
        long executeDueTasksWindowEndTime = System.currentTimeMillis() + maxExecuteTasksTimeMillis.get();
        Iterator<TimerTask> i = executeNow.iterator();
        while (i.hasNext() && executeDueTasksWindowEndTime > System.currentTimeMillis()) {
            TimerTask next = i.next();
            try {

                if (executeTask(next)) {
                    piTimerQueue.put(next);
                }

            } catch (RuntimeException e) {
                LOG.error("Error while trying to execute due task: " + next, e);
                break;
            }
        }

        while (i.hasNext()) {
            // add back any unfinished tasks.
            piTimerQueue.put(i.next());
        }
    }

    @Override
    public boolean executeTask(final TimerTask next) {
        executeTaskLoggerConfiguration.setCallableToRun(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                if (LOG.isDebugEnabled())
                    LOG.debug("Execute Task: " + next);
                boolean result = next.execute(timeSource);

                return result;
            }

        });
        executeTaskLoggerConfiguration.setLogHeader(getClass().getSimpleName() + ".executeTask(" + next + ")");

        try {
            return executeTaskLogger.call();
        } catch (Exception e) {
            LOG.warn("Error executing task: " + next);
            return false;
        }
    }

    @Override
    public void addTask(final TimerTask task) {
        addTaskLoggerConfiguration.setCallableToRun(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                callAddTask(task);
                return null;
            }
        });
        addTaskLoggerConfiguration.setLogHeader(getClass().getSimpleName() + ".addTask(" + task + ")");
        try {
            addTaskLogger.call();
        } catch (Exception e) {
            LOG.warn("Exception during add task for task " + task, e);
        }
    }

    private void callAddTask(TimerTask task) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("addTask(%s)", task));
        if (!piTimerQueue.add(task)) {
            LOG.error("ERROR: Got false while enqueueing task " + task + "!");
        } else {

            task.setSelectorManager(this);
        }

        // need to interrupt thread if waiting too long in selector
        if (select) {
            // using the network
            if (wakeupTime >= task.scheduledExecutionTime()) {
                // we need to wake up the selector because it's going to sleep too long
                wakeup();
            }
        } else {
            // using the simulator
            if (task.scheduledExecutionTime() == getNextTaskExecutionTime()) {
                // we need to wake up the selector because we are now the newest
                // shortest wait, and may be delaying because of a later event
                wakeup();
            }
        }
    }

    @Override
    public void removeTask(TimerTask task) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("removeTask(%s)", task));
        boolean removed = piTimerQueue.remove(task);
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Task %s removed %s", task, removed));
    }

    @Override
    public long getNextTaskExecutionTime() {
        if (piTimerQueue.size() > 0) {
            TimerTask next = (TimerTask) piTimerQueue.peek();
            return next.scheduledExecutionTime();
        }
        return -1;
    }

    /*
     * Selection code
     * 
     */

    @Override
    public void doSelections() throws IOException {
        try {
            doSelectionsLogger.call();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void callDoSelectionsInSelectorManager() throws IOException {
        super.doSelections();
    }

    @Override
    protected int select(int time) throws IOException {
        return select();
    }

    protected int select() throws IOException {
        try {
            if (!invocations.isEmpty() || !modifyKeys.isEmpty())
                return selector.selectNow();

            return selector.select(selectorSleepTimeMillis.longValue());
        } catch (CancelledKeyException cce) {
            LOG.warn("CCE: cause:", cce.getCause());
            throw cce;
        } catch (IOException e) {
            if (e.getMessage().indexOf("Interrupted system call") >= 0) {
                LOG.warn("Got interrupted system call, continuing anyway...");
                return 1;
            } else {
                throw e;
            }
        }
    }

    @Override
    protected SelectionKey getModifyKey() {
        return piModifiedKeys.poll();
    }

    /* (non-Javadoc)
     * @see com.bt.pi.core.pastry_override.PiSelectorManager#modifyKey(java.nio.channels.SelectionKey)
     */

    @Override
    public void modifyKey(SelectionKey key) {
        piModifiedKeys.add(key);
        wakeup();
    }

    @Override
    public void wakeup() {
        selector.wakeup();
        if (Thread.holdsLock(this)) {
            this.notifyAll();
        }
    }

    @Override
    public void run() {
        LOG.info("SelectorManager -- " + instance + " starting...");

        pilastTime = timeSource.currentTimeMillis();
        // loop while waiting for activity
        while (running) {
            try {
                if (useLoopListeners)
                    notifyLoopListeners();

                // NOTE: This is so we aren't always holding the selector lock when we
                // get context switched
                Thread.yield();
                executeDueTasks();
                onLoop();
                doInvocations();
                if (select) {
                    doSelections();

                    select();

                    if (cancelledKeys.size() > 0) {
                        Iterator<SelectionKey> i = cancelledKeys.iterator();

                        while (i.hasNext())
                            ((SelectionKey) i.next()).cancel();

                        cancelledKeys.clear();

                        // now, hack to make sure that all cancelled keys are actually
                        // cancelled (dumb)
                        selector.selectNow();
                    }
                } // if select
            } catch (Throwable t) {
                LOG.error("ERROR (SelectorManager.run): ", t);
                environment.getExceptionStrategy().handleException(this, t);
                // System.exit(-1);
            }
        } // while(running)
        invocations.clear();
        cancelledKeys.clear();
        timerQueue.clear();
        piInvocations.clear();
        piTimerQueue.clear();
        piTimerQueue = null;
        piInvocations = null;
        invocations = null;
        cancelledKeys = null;
        timerQueue = null;
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ioe) {
            LOG.warn("Error cancelling selector:", ioe);
        }
        LOG.info("Selector " + instance + " shutting down.");
    }

    public int getMaxInvocationTimeMillis() {
        return maxInvocationTimeMillis.get();
    }

    public int getMaxExecuteTasksTimeMillis() {
        return maxExecuteTasksTimeMillis.get();
    }

    public int getSelectorSleepTimeMillis() {
        return selectorSleepTimeMillis.get();
    }

    @Property(key = "selector.thread.loop.sleep.time.millis", defaultValue = THREE_HUNDRED)
    public void setSelectorThreadLoopSleepMillis(int timeInMillis) {
        selectorSleepTimeMillis.set(timeInMillis);

    }

    @Property(key = "max.invocation.time.millis", defaultValue = EIGHT_HUNDRED)
    public void setMaxInvocationTimeMillis(int timeInMillis) {
        maxInvocationTimeMillis.set(timeInMillis);

    }

    @Property(key = "max.execute.due.tasks.time.millis", defaultValue = EIGHT_HUNDRED)
    public void setMaxExecuteDueTaskTimeMillis(int timeInMillis) {
        maxExecuteTasksTimeMillis.set(timeInMillis);

    }

    public long getPilastTime() {
        return pilastTime;
    }

    public void setPilastTime(long aTime) {
        this.pilastTime = aTime;
    }

    /**
     * @return the piLoopObservers
     */
    protected CopyOnWriteArrayList<LoopObserver> getPiLoopObservers() {
        return piLoopObservers;
    }

    /**
     * @return the piTimerQueue
     */
    protected PriorityBlockingQueue<TimerTask> getPiTimerQueue() {
        return piTimerQueue;
    }

    /**
     * @return the piModifiedKeys
     */
    protected ConcurrentLinkedQueue<SelectionKey> getPiModifiedKeys() {
        return piModifiedKeys;
    }

    /*
     * Only used for testing!
     */
    protected void setSelector(Selector aSelector) {
        selector = aSelector;
    }

    public void setPort(int port) {
        this.port = port;
        renameInstance();

    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
        renameInstance();
    }

    private void renameInstance() {
        this.instance = inetAddress.getHostAddress() + "." + port;
        setName("Selector Thread -- " + instance);
    }

    public void setParameters(Parameters someParameters) {
        this.parameters = someParameters;

    }

    public void setLogManager(LogManager alogManager) {
        this.logManager = alogManager;
    }

    @Override
    public TimerTask schedule(TimerTask task) {
        addTask(task);
        return task;
    }

}

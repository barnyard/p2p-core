//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.util;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    /**
     * Constant representing fifty thousand nanoseconds.
     */
    private static final int FIFTY_THOUSAND_NANOSECONDS = 50000;

    /**
     * Constant representing one hundred thousand nanoseconds
     */
    private static final int HUNDRED_THOUSAND_NANOSECONDS = 100000;

    /**
     * Constant for 10.
     */
    private static final int TEN = 10;

    /**
     * Constant string for inbound logging direction.
     */
    private static final String INBOUND = "INBOUND";

    /**
     * Constant string for outbound logging direction.
     */
    private static final String OUTBOUND = "OUTBOUND";

    public LoggingAspect() {
    }

    /**
     * Logs the start and end of inbound method calls for end-to-end purposes. If an exception is thrown by the method,
     * then an end inbound message is logged along with the exception details.
     * 
     * @param thisJoinPoint
     *            the join point during the execution of the method.
     * @return the result of the method call.
     * @throws Throwable
     *             if an exception occurs during the method call.
     */
    @Around("com.bt.pi.core.util.Pointcuts.inboundLogging()")
    public Object traceInbound(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        return trace(thisJoinPoint, INBOUND);
    }

    /**
     * Logs the start and end of outbound method calls for end-to-end purposes. If an exception is thrown by the method,
     * then an end outbound message is logged along with the exception details.
     * 
     * @param thisJoinPoint
     *            the join point during the execution of the method.
     * @return the result of the method call.
     * @throws Throwable
     *             if an exception occurs during the method call.
     */
    @Around("com.bt.pi.core.util.Pointcuts.outboundLogging()")
    public Object traceOutbound(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        return trace(thisJoinPoint, OUTBOUND);
    }

    /**
     * Logs the start and end of method calls with the specified direction for end-to-end purposes. If an exception is
     * thrown by the method, then an end direction message is logged along with the exception details.
     * 
     * @param thisJoinPoint
     *            the join point during the execution of the method.
     * @param direction
     *            the direction of the message being logged.
     * @return the result of the method call.
     * @throws Throwable
     *             if an exception occurs during the method call.
     */
    private Object trace(ProceedingJoinPoint thisJoinPoint, String direction) throws Throwable {
        Object result;
        long start = System.nanoTime();
        enter(thisJoinPoint, direction);
        try {
            result = thisJoinPoint.proceed();
        } catch (Throwable t) {
            long end = System.nanoTime();
            exception(thisJoinPoint.getStaticPart(), t, direction, start, end);
            throw t;
        }
        long end = System.nanoTime();
        exit(thisJoinPoint.getStaticPart(), direction, start, end, result);
        return result;
    }

    /**
     * Checks whether INFO level logging has been enabled for the specified logger.
     * 
     * @param logger
     *            the logger to check.
     * @return <code>true</code> if logging is enabled for INFO level; <code>false</code> otherwise.
     */
    private boolean isLoggingEnabled(Logger logger) {
        return logger.isEnabledFor(Level.INFO);
    }

    /**
     * Returns the name of the logger. This is the class enclosing the specified join point
     * 
     * @param sjp
     *            the static part of the join point during the execution of the method.
     * @return the name of the logger.
     */
    protected String getLoggerName(JoinPoint.StaticPart sjp) {
        return sjp.getSignature().getDeclaringTypeName();
    }

    /**
     * Logs the start of method calls with the specified direction for end-to-end purposes.
     * 
     * @param jp
     *            the join point during the execution of the method.
     * @param direction
     *            the direction of the message being logged.
     */
    protected void enter(JoinPoint jp, String direction) {
        Logger logger = Logger.getLogger(getLoggerName(jp.getStaticPart()));
        if (isLoggingEnabled(logger)) {
            CodeSignature signature = (CodeSignature) jp.getSignature();
            String threadName = Thread.currentThread().getName();
            String declaringTypeName = signature.getDeclaringTypeName();
            Class<?>[] argTypes = signature.getParameterTypes();
            Object[] args = jp.getArgs();
            // search for a byte array type in argTypes
            // if it contains 1 or more byteArrays replace the content of the
            // array args at the appropriate index
            // with "byte[size]"
            processArgs(argTypes, args);
            String argsString = Arrays.deepToString(args);
            String signName = signature.getName();
            logger.info(String.format("%s: START %s: %s.%s(%s)", threadName, direction, declaringTypeName, signName, argsString));
        }
    }

    /**
     * Processes the arguments of the method call at the start. Arguments of type <code>byte[]</code> are replaced with
     * a string indicating the size of the byte array for logging purposes.
     * 
     * @param argTypes
     *            array of class objects representing the argument types.
     * @param args
     *            array of objects representing the arguments.
     */
    private void processArgs(Class<?>[] argTypes, Object[] args) {
        for (int c = 0; c < argTypes.length; c++)
            if (args[c] != null && argTypes[c].equals(byte[].class))
                args[c] = String.format("byte[%1$s]", ((byte[]) args[c]).length);
    }

    /**
     * Logs the end of method calls with the specified direction for end-to-end purposes.
     * 
     * @param sjp
     *            the static part of the join point during the execution of the method.
     * @param direction
     *            the direction of the message being logged.
     * @param start
     *            the start time of the method execution in nanoseconds.
     * @param end
     *            the end time of the method execution in nanoseconds.
     */
    protected void exit(JoinPoint.StaticPart sjp, String direction, long start, long end, Object result) {
        long tenthmillis = (end - start + FIFTY_THOUSAND_NANOSECONDS) / HUNDRED_THOUSAND_NANOSECONDS;
        Logger logger = Logger.getLogger(getLoggerName(sjp));
        if (isLoggingEnabled(logger)) {
            CodeSignature signature = (CodeSignature) sjp.getSignature();
            logger.info(String.format("%s: END %s: %s.%s: Time: %d.%d ms, Result: %s", Thread.currentThread().getName(), direction, signature.getDeclaringTypeName(), signature.getName(), tenthmillis / TEN, tenthmillis % TEN, result));
        }
    }

    /**
     * Logs the end of method calls with the specified direction and throwable for end-to-end purposes.
     * 
     * @param sjp
     *            the static part of the join point during the execution of the method.
     * @param t
     *            the throwable.
     * @param direction
     *            the direction of the message being logged.
     * @param start
     *            the start time of the method execution in nanoseconds.
     * @param end
     *            the end time of the method execution in nanoseconds.
     */
    protected void exception(JoinPoint.StaticPart sjp, Throwable t, String direction, long start, long end) {
        long tenthmillis = (end - start + FIFTY_THOUSAND_NANOSECONDS) / HUNDRED_THOUSAND_NANOSECONDS;
        Logger logger = Logger.getLogger(getLoggerName(sjp));
        if (isLoggingEnabled(logger)) {
            CodeSignature signature = (CodeSignature) sjp.getSignature();
            logger.info(String.format("%s: END %s with Exception: %s.%s: Time: %d.%d ms", Thread.currentThread().getName(), direction, signature.getDeclaringTypeName(), signature.getName(), tenthmillis / TEN, tenthmillis % TEN), t);
        }
    }
}

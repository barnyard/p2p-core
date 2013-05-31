package com.bt.pi.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogIfSlowAspect {

    public static final long DEFAULT_TIMEOUT = 100;

    public static final long DEFAULT_TIME_BETWEEN_LOGS = 10000;

    private static final Log LOG = LogFactory.getLog(LogIfSlowAspect.class);
    private volatile long lastUsed;

    public LogIfSlowAspect() {

    }

    @Around("@annotation(logIfSlow)")
    public void logIfSlow(final ProceedingJoinPoint pjp, LogIfSlow logIfSlow) throws Throwable {
        processMethod(pjp, logIfSlow, getClassAndMethodInvoked(pjp.getStaticPart()));

    }

    @Around("@annotation(logIfSlow) && args(obj)")
    public void logIfSlow(final ProceedingJoinPoint pjp, LogIfSlow logIfSlow, Object obj) throws Throwable {
        processMethod(pjp, logIfSlow, getClassAndMethodInvoked(pjp.getStaticPart()) + "(" + obj + ")");

    }

    private void processMethod(final ProceedingJoinPoint pjp, LogIfSlow logIfSlow, String heading) throws Throwable {
        final long startTime = System.currentTimeMillis();
        try {
            pjp.proceed();
        } finally {
            long methodExecutionTime = System.currentTimeMillis() - startTime;
            if (methodExecutionTime > logIfSlow.timeOut() && (lastUsed == 0 || (startTime - lastUsed) > logIfSlow.waitingTimeBetweenLogs())) {
                final long now = System.currentTimeMillis();
                lastUsed = now;
                logTime(heading, now - startTime);

            }
        }
    }

    protected void logTime(String heading, long time) {
        LOG.info(heading + " took: " + time + " millis");
    }

    private String getClassAndMethodInvoked(JoinPoint.StaticPart sjp) {
        return sjp.getSignature().getDeclaringTypeName() + "." + sjp.getSignature().getName();
    }

}
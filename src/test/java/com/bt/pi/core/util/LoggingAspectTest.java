package com.bt.pi.core.util;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingAspectTest {

    private static final String TEST_TYPE_NAME = "TEST_TYPE_NAME";

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private JoinPoint.StaticPart staticPart;

    @Mock
    private org.aspectj.lang.Signature signature;

    private LoggingAspect loggingAspect = new LoggingAspect();

    @Before
    public void setup() {
        when(pjp.getStaticPart()).thenReturn(staticPart);
        when(staticPart.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn(TEST_TYPE_NAME);
    }

    @Test
    public void shouldTraceInbound() throws Throwable {
        loggingAspect.traceInbound(pjp);
    }

    @Test
    public void shouldTraceOutbound() throws Throwable {
        loggingAspect.traceOutbound(pjp);
    }

    @Test
    public void shouldGetLoggerName() {
        assertEquals(TEST_TYPE_NAME, loggingAspect.getLoggerName(staticPart));
    }

    @Test
    public void shouldEnter() {
        loggingAspect.enter(pjp, "direction");
    }
}

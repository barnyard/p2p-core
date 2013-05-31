package com.bt.pi.core.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BlockingAspect {

    private static final String SELECTOR_THREAD_NAME = "Selector Thread";

    public BlockingAspect() {

    }

    /**
     * This pointcut can be used to mark classes that contain blocking methods.
     * 
     * @param jp
     * @param blocking
     */
    @Before("@annotation(com.bt.pi.core.util.Blocking)")
    public void throwExceptionIfRunningInSelectorThread(JoinPoint jpg) {
        checkIfOperationRunningInSelectorClass();
    }

    private void checkIfOperationRunningInSelectorClass() {
        String threadName = Thread.currentThread().getName();
        if (threadName.contains(SELECTOR_THREAD_NAME)) {
            throw new BlockingOperationCannotRunInSelectorThreadException();
        }
    }

}

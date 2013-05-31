//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.util;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class Pointcuts {
    public Pointcuts() {

    }

    @Pointcut("execution(public * *(..))")
    public void anyPublicOperation() {
    }

    @Pointcut("within(com.bt.pi.core.management.*)")
    public void inManagementLayer() {
    }

    @Pointcut("within(com.bt.pi.core.unused.*)")
    public void unused() {
    }

    @Pointcut("within(rice.Continuation)")
    public void inRice() {
    }

    @Pointcut("anyPublicOperation() && (inManagementLayer() || inRice())")
    public void inboundLogging() {
    }

    @Pointcut("anyPublicOperation() && unused()")
    public void outboundLogging() {
    }
}
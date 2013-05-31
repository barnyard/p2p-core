package com.bt.pi.core.application.resource.leased;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.bt.pi.core.scope.NodeScope;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LeasedAllocatedResource {
    NodeScope allocationRecordScope();

    String allocationRecordUri();
}

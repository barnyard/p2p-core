package com.bt.pi.core.util.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class AnnotationUtils {
    static {
        new AnnotationUtils(); // for emma
    }

    private AnnotationUtils() {
    }

    public static List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new ArrayList<Method>();
        for (Method m : clazz.getMethods()) {
            for (Annotation a : m.getAnnotations()) {
                if (annotationClass.isInstance(a)) {
                    methods.add(m);
                }
            }
        }
        return methods;
    }
}

package com.bt.pi.core.util.annotation;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

public class AnnotationUtilsTest {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotation {
    }

    public class MyAnnotatedClass {
        @TestAnnotation
        public void someMethod() {
        }
    }

    @Test
    public void shouldFindAnnotatedMethodsForClass() {
        // act
        List<Method> res = AnnotationUtils.findAnnotatedMethods(MyAnnotatedClass.class, TestAnnotation.class);

        // assert
        assertEquals(1, res.size());
        assertEquals("someMethod", res.get(0).getName());
    }
}

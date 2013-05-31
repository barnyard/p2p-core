package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.bt.pi.core.scope.NodeScope;

public class EntityAnnotationTest {

    @Test
    public void testAnnotationsAreAvailabileAtRuntime() {
        // act
        TestEntity entity = new TestEntity();

        // assert
        assertEquals(2, entity.getClass().getAnnotations().length);
        assertEquals(NodeScope.REGION, entity.getClass().getAnnotation(EntityScope.class).scope());
        assertNotNull(entity.getClass().getAnnotation(Backupable.class));
    }
}

@EntityScope(scope = NodeScope.REGION)
@Backupable
class TestEntity {
    public TestEntity() {

    }
}

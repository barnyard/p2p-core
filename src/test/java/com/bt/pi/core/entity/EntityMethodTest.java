package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.core.entity.EntityMethod;

public class EntityMethodTest {

    @Test
    public void testGet() {
        assertEquals(EntityMethod.CREATE, EntityMethod.get("create"));
    }
}

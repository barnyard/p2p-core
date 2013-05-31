package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.bt.pi.core.entity.EntityResponseCode;

public class EntityResponseCodeTest {

    @Test
    public void testGet() {
        assertEquals(EntityResponseCode.OK, EntityResponseCode.get(200));
    }
}

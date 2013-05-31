package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.bt.pi.core.entity.EntityResponseCode;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityResponse;

public class PiEntityResponseTest {

    @Test
    public void testGettersAndSetters() {
        // setup
        PiEntityResponse response = new PiEntityResponse();
        PiEntity entity = mock(PiEntity.class);

        // act
        response.setEntity(entity);
        response.setEntityResponseCode(EntityResponseCode.OK);

        // assert
        assertEquals(entity, response.getEntity());
        assertEquals(EntityResponseCode.OK, response.getEntityResponseCode());
    }
}

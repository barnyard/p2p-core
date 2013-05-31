package com.bt.pi.core.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class PiEntityBaseTest {
    private PiEntityBase piEntityBase;

    @Before
    public void before() {
        piEntityBase = new PiEntityBase() {
            @Override
            public String getUrl() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public String getUriScheme() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Test
    public void shouldIncrementVersion() {
        // act
        piEntityBase.incrementVersion();

        // assert
        assertEquals(1L, piEntityBase.getVersion());
    }
}

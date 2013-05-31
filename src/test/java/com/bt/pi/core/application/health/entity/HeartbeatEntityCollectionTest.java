package com.bt.pi.core.application.health.entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HeartbeatEntityCollectionTest {
    @Test
    public void shouldGetType() throws Exception {
        // setup
        HeartbeatEntityCollection heartbeatEntityCollection = new HeartbeatEntityCollection();

        // act
        String result = heartbeatEntityCollection.getType();

        // assert
        assertThat(result, equalTo("HeartbeatEntityCollection"));
    }
}

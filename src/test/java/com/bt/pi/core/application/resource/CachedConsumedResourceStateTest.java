package com.bt.pi.core.application.resource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import rice.p2p.commonapi.Id;

import com.bt.pi.core.entity.PiEntity;

public class CachedConsumedResourceStateTest {
    private CachedConsumedResourceState<Id> cachedConsumedResourceState;
    private Id id;
    private PiEntity piEntity;

    @Before
    public void before() {
        id = mock(Id.class);
        piEntity = mock(PiEntity.class);
        cachedConsumedResourceState = new CachedConsumedResourceState<Id>(id, piEntity);
    }

    @Test
    public void testConstructor() throws Exception {
        // act
        cachedConsumedResourceState = new CachedConsumedResourceState<Id>(id, piEntity);

        // assert
        assertThat(cachedConsumedResourceState.getId(), equalTo(id));
        assertThat(((PiEntity) cachedConsumedResourceState.getEntity()), equalTo(piEntity));
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        // act
        ConsumedResourceState<Id> watchedSharedResource = new ConsumedResourceState<Id>(id);

        // assert
        assertThat(watchedSharedResource.getId(), equalTo(id));
    }

    @Test
    public void shouldGetAndSetEntity() {
        // act
        cachedConsumedResourceState.setEntity(piEntity);

        // assert
        assertEquals(piEntity, cachedConsumedResourceState.getEntity());
    }

    @Test
    public void shouldReturnTrueWhenRegisteringFirst() {
        // act
        boolean res = cachedConsumedResourceState.registerConsumer("1");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldReturnTrueWhenRegisteringSecond() {
        // setup
        cachedConsumedResourceState.registerConsumer("1");

        // act
        boolean res = cachedConsumedResourceState.registerConsumer("2");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldReturnTrueWhenDeregisteringLast() {
        // setup
        cachedConsumedResourceState.registerConsumer("1");

        // act
        boolean res = cachedConsumedResourceState.deregisterConsumer("1");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldReturnFalseWhenDeregisteringNonLast() {
        // setup
        cachedConsumedResourceState.registerConsumer("1");
        cachedConsumedResourceState.registerConsumer("2");

        // act
        boolean res = cachedConsumedResourceState.deregisterConsumer("2");

        // assert
        assertFalse(res);
    }

    @Test
    public void shouldIgnoreDuplicateConsumers() {
        // setup
        cachedConsumedResourceState.registerConsumer("1");
        cachedConsumedResourceState.registerConsumer("1");

        // act
        boolean res = cachedConsumedResourceState.deregisterConsumer("1");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldReturnTrueWhenDeregisteringNonExistent() {
        // act
        boolean res = cachedConsumedResourceState.deregisterConsumer("1");

        // assert
        assertTrue(res);
    }

}

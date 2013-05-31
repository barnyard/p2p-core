package com.bt.pi.core.entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class PersistablePiEntityMappingsTest {
    private PersistablePiEntityMappings persistablePiEntityMappings;

    @Before
    public void setup() {
        persistablePiEntityMappings = new PersistablePiEntityMappings();
    }

    @Test
    public void shouldGetPersistablePiEntityMappings() throws Exception {
        // setup
        Collection<PersistablePiEntityMapping> mappings = new ArrayList<PersistablePiEntityMapping>();

        // act
        persistablePiEntityMappings.setPersistablePiEntityMappings(mappings);

        // assert
        assertThat(persistablePiEntityMappings.getPersistablePiEntityMappings(), equalTo(mappings));
    }
}

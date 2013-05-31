package com.bt.pi.core.entity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class PersistablePiEntityMappingTest {
    private String type = "type";
    private String scheme = "scheme";
    private int typeCode = 42;

    private PersistablePiEntityMapping persistablePiEntityMapping;

    @Before
    public void setup() {
        persistablePiEntityMapping = new PersistablePiEntityMapping();
    }

    @Test
    public void gettersAndSetters() throws Exception {
        // act
        persistablePiEntityMapping.setType(type);
        persistablePiEntityMapping.setScheme(scheme);
        persistablePiEntityMapping.setTypeCode(typeCode);

        // assert
        assertThat(persistablePiEntityMapping.getType(), equalTo(type));
        assertThat(persistablePiEntityMapping.getScheme(), equalTo(scheme));
        assertThat(persistablePiEntityMapping.getTypeCode(), equalTo(typeCode));
    }

    @Test
    public void constructorWithArguments() throws Exception {
        // act
        persistablePiEntityMapping = new PersistablePiEntityMapping(type, scheme, typeCode);

        // assert
        assertThat(persistablePiEntityMapping.getType(), equalTo(type));
        assertThat(persistablePiEntityMapping.getScheme(), equalTo(scheme));
        assertThat(persistablePiEntityMapping.getTypeCode(), equalTo(typeCode));
    }
}

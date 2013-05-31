/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.core.entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class PiEntityCollectionTest {
    private PiEntityCollection<PiEntity> piEntityCollection;
    private Collection<PiEntity> ents;

    @Before
    public void doBefore() {
        piEntityCollection = new PiEntityCollection<PiEntity>() {
            @Override
            public String getType() {
                return "test";
            }

            @Override
            public String getUriScheme() {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Test
    public void getUrlShouldReturnNull() {
        assertNull(piEntityCollection.getUrl());
    }

    @Test
    public void entitiesShouldBeGettableAndSettable() {
        ents = new ArrayList<PiEntity>();

        piEntityCollection.setEntities(ents);

        assertSame(ents, piEntityCollection.getEntities());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void equalsShouldReturnTrueEntitiesCollectionIsEqual() {
        ents = mock(Collection.class);
        piEntityCollection.setEntities(ents);
        PiEntityCollection<PiEntity> col2 = new PiEntityCollection<PiEntity>() {
            @Override
            public String getType() {
                return "test";
            }

            @Override
            public String getUriScheme() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        col2.setEntities(ents);

        assertTrue(piEntityCollection.equals(col2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void equalsShouldReturnFalseEntitiesCollectionIsNotEqual() {
        piEntityCollection.setEntities(mock(Collection.class));
        PiEntityCollection<PiEntity> col2 = new PiEntityCollection<PiEntity>() {
            @Override
            public String getType() {
                return "test";
            }

            @Override
            public String getUriScheme() {
                // TODO Auto-generated method stub
                return null;
            }
        };
        col2.setEntities(mock(Collection.class));

        assertFalse(piEntityCollection.equals(col2));
    }

    @Test
    public void equalsShouldReturnFalseIfNotPiEntityCollection() {
        assertFalse(piEntityCollection.equals(mock(Object.class)));
    }

    @Test
    public void equalsShouldReturnFalseIfNull() {
        assertFalse(piEntityCollection.equals(null));
    }

    @Test
    public void hashCodeShouldBeGeneratedFromFields() {

        piEntityCollection.hashCode();
    }
}

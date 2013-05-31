package com.bt.pi.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.application.health.entity.HeartbeatEntity;
import com.bt.pi.core.application.health.entity.HeartbeatEntityCollection;
import com.bt.pi.core.application.health.entity.LogMessageEntity;
import com.bt.pi.core.application.health.entity.LogMessageEntityCollection;
import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.DuplicateEntityException;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.entity.PiEntityBase;

public class KoalaPiEntityFactoryTest {
    private KoalaJsonParser parser;
    private KoalaPiEntityFactory koalaEntityFactory;

    @Before
    public void setup() {
        koalaEntityFactory = new KoalaPiEntityFactory();

        parser = new KoalaJsonParser();
        koalaEntityFactory.setKoalaJsonParser(parser);
    }

    @Test
    public void testLargeJsonToGetPiEntity() {
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new LogMessageEntityCollection());
        koalaEntityFactory.setPiEntityTypes(entityTypes);

        Collection<LogMessageEntity> logMessageEntities = new ArrayList<LogMessageEntity>();
        for (int i = 0; i < 1000; i++)
            logMessageEntities.add(createLogMessageEntity("hostname" + i));

        LogMessageEntityCollection logMessageEntityCollection = new LogMessageEntityCollection();
        logMessageEntityCollection.setEntities(logMessageEntities);

        String json = parser.getJson(logMessageEntityCollection);

        LogMessageEntityCollection result = (LogMessageEntityCollection) koalaEntityFactory.getPiEntity(json);

        assertEquals(logMessageEntities.size(), result.getEntities().size());
    }

    @Test
    public void testLargeJsonToGetHeartbeatEntity() {
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new HeartbeatEntityCollection());
        koalaEntityFactory.setPiEntityTypes(entityTypes);

        Collection<HeartbeatEntity> heartbeatMessageEntities = new ArrayList<HeartbeatEntity>();
        for (int i = 0; i < 4; i++)
            heartbeatMessageEntities.add(createHeartbeatEntity("hostname" + i));

        HeartbeatEntityCollection heartbeatEntityCollection = new HeartbeatEntityCollection();
        heartbeatEntityCollection.setEntities(heartbeatMessageEntities);

        String json = parser.getJson(heartbeatEntityCollection);

        HeartbeatEntityCollection result = (HeartbeatEntityCollection) koalaEntityFactory.getPiEntity(json);

        assertEquals(heartbeatMessageEntities.size(), result.getEntities().size());
    }

    private LogMessageEntity createLogMessageEntity(String message) {
        return new LogMessageEntity(System.currentTimeMillis(), "this is a test message and we are going to keep testing " + message, "test", "", "");
    }

    private HeartbeatEntity createHeartbeatEntity(String hostname) {
        HeartbeatEntity entity = new HeartbeatEntity("1231432432423423423223432432343242");
        entity.setHostname("localhost");

        Map<String, Long> theDiskSpace = new HashMap<String, Long>();
        for (int i = 0; i < 100; i++) {
            theDiskSpace.put("a" + i, 1L);
        }

        entity.setDiskSpace(theDiskSpace);

        entity.setLeafSet(Arrays.asList("leafset 1", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2", "leafset 2"));

        entity.setMemoryDetails(theDiskSpace);

        return entity;
    }

    @Test(expected = DuplicateEntityException.class)
    public void testExceptionThrowOnDuplicatePersistedEntity() {
        // setup
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new PersistedEntity1());
        entityTypes.add(new PersistedEntity2());

        // act
        try {
            koalaEntityFactory.setPiEntityTypes(entityTypes);
        } catch (DuplicateEntityException e) {
            throw e;
        }
    }

    @Test(expected = DuplicateEntityException.class)
    public void testExceptionThrowOnDuplicateEntity() {
        // setup
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new DuplicateEntity());
        entityTypes.add(new DuplicateEntity());

        // act
        try {
            koalaEntityFactory.setPiEntityTypes(entityTypes);
        } catch (DuplicateEntityException e) {
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionOnNullScheme() {
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new DuplicateEntity() {
            @Override
            public String getUriScheme() {
                return null;
            }
        });

        // act
        try {
            koalaEntityFactory.setPiEntityTypes(entityTypes);
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionOnNullType() {
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new DuplicateEntity() {
            @Override
            public String getType() {
                return null;
            }
        });

        // act
        try {
            koalaEntityFactory.setPiEntityTypes(entityTypes);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Test
    public void shouldConvertIdxSchemeToFullUrl() {
        // setup
        ArrayList<PiEntity> entityTypes = new ArrayList<PiEntity>();
        entityTypes.add(new BackupablePersistableIndexEntity1());
        entityTypes.add(new BackupablePersistableIndexEntity2());

        // act
        koalaEntityFactory.setPiEntityTypes(entityTypes);

        // assert
        assertTrue(koalaEntityFactory.isBackupable("idx:BackupablePersistableIndexEntity1"));
        assertTrue(koalaEntityFactory.isBackupable("idx:BackupablePersistableIndexEntity2"));
    }

    @Backupable
    static class BackupablePersistableIndexEntity1 extends PiEntityBase {
        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return "idx:" + getClass().getSimpleName();
        }

        @Override
        public String getUriScheme() {
            return "idx";
        }
    }

    @Backupable
    static class BackupablePersistableIndexEntity2 extends PiEntityBase {
        @Override
        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getUrl() {
            return "idx:" + getClass().getSimpleName();
        }

        @Override
        public String getUriScheme() {
            return "idx";
        }
    }

    static class PersistedEntity1 extends PiEntityBase {
        @Override
        public String getType() {
            return "PersistedEntity1";
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return "PersistedEntity";
        }
    }

    static class PersistedEntity2 extends PiEntityBase {
        @Override
        public String getType() {
            return "PersistedEntity2";
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return "PersistedEntity";
        }
    }

    static class DuplicateEntity extends PiEntityBase {
        static int count;

        @Override
        public String getType() {
            return "DuplicateEntity";
        }

        @Override
        public String getUrl() {
            return null;
        }

        @Override
        public String getUriScheme() {
            return getType() + count++;
        }
    }
}

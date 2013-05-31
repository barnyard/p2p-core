package com.bt.pi.core.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.DuplicateEntityException;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.exception.KoalaException;

@Component
public class KoalaPiEntityFactory {
    private static final Log LOG = LogFactory.getLog(KoalaPiEntityFactory.class);
    private static final String PI_ENTITY_MAPPING_FOUND = "PiEntity Mapping Found: ";
    private static final String JSON_PARSE_EXCEPTION_MESSAGE = "Unable to create PiEntity of type %s";
    private Map<String, Class<? extends PiEntity>> piEntityTypeMapping;
    private KoalaJsonParser koalaJsonParser;
    private Map<String, Boolean> schemeToBackupableMapping;
    private Map<String, String> schemeToTypeMapping;

    public KoalaPiEntityFactory() {
        piEntityTypeMapping = null;
        koalaJsonParser = null;
        schemeToBackupableMapping = new HashMap<String, Boolean>();
        schemeToTypeMapping = new HashMap<String, String>();
    }

    @Resource
    public void setKoalaJsonParser(KoalaJsonParser jsonParser) {
        koalaJsonParser = jsonParser;
    }

    public PiEntity getPiEntity(String json) {
        PiEntity entity = null;
        if (json != null) {
            String type = null;
            try {
                Class<? extends PiEntity> piEntityType = piEntityTypeMapping.get(koalaJsonParser.getValueOfScalar(json, PiEntity.TYPE_PARAM));
                if (piEntityType != null)
                    type = piEntityType.getName();
            } catch (KoalaException e) {
                LOG.error(e.getMessage(), e);
            }
            if (StringUtils.isBlank(type)) {
                // TODO: throw an exception instead
                LOG.error(String.format("Recognizable type not present in JSONObject: %s. Returning null result", json));
            } else {
                entity = createPiEntityObject(type, json);
            }
        }
        return entity;
    }

    private PiEntity createPiEntityObject(String type, String json) {
        PiEntity entity = null;
        try {
            entity = (PiEntity) koalaJsonParser.getObject(json, Class.forName(type));
        } catch (SecurityException e) {
            LOG.error(String.format(JSON_PARSE_EXCEPTION_MESSAGE, type), e);
        } catch (ClassNotFoundException e) {
            LOG.error(String.format(JSON_PARSE_EXCEPTION_MESSAGE, type), e);
        } catch (IllegalArgumentException e) {
            LOG.error(String.format(JSON_PARSE_EXCEPTION_MESSAGE, type), e);
        }
        return entity;
    }

    public String getJson(PiEntity data) {
        return koalaJsonParser.getJson(data);
    }

    @Resource
    public void setPiEntityTypes(List<PiEntity> piEntityTypes) {
        LOG.debug(String.format("setPiEntityTypes(%s)", piEntityTypes));
        piEntityTypeMapping = new HashMap<String, Class<? extends PiEntity>>();

        // create the maps
        for (PiEntity entity : piEntityTypes) {
            LOG.debug(PI_ENTITY_MAPPING_FOUND + entity.getType() + " for entity: " + entity);
            String scheme = entity.getUriScheme();
            if (null == scheme)
                throw new IllegalArgumentException(String.format("Entity: %s cannot have null URI scheme", entity.getClass().getName()));

            if ("idx".equals(scheme))
                scheme = entity.getUrl();

            String type = entity.getType();
            if (null == type)
                throw new IllegalArgumentException(String.format("Entity: %s cannot have null type", entity.getClass().getName()));

            if (schemeToTypeMapping.containsKey(scheme))
                throw new DuplicateEntityException(String.format("Entity: %s cannot use scheme %s as %s already uses it.", entity.getClass().getName(), scheme, schemeToTypeMapping.get(scheme)));

            schemeToTypeMapping.put(scheme, type);

            boolean backupable = entity.getClass().isAnnotationPresent(Backupable.class);
            schemeToBackupableMapping.put(scheme, backupable);

            if (piEntityTypeMapping.containsKey(type))
                throw new DuplicateEntityException(String.format("Entity: %s cannot use type %s as %s already uses it.", entity.getClass().getName(), type, piEntityTypeMapping.get(type)));
            piEntityTypeMapping.put(type, entity.getClass());
        }
    }

    public boolean isBackupable(String scheme) {
        if (schemeToBackupableMapping.containsKey(scheme))
            return schemeToBackupableMapping.get(scheme);
        return false;
    }
}

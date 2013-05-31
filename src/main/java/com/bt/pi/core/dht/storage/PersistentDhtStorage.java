package com.bt.pi.core.dht.storage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import rice.environment.Environment;
import rice.p2p.commonapi.Id;
import rice.p2p.util.ReverseTreeMap;

import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.parser.KoalaPiEntityFactory;
import com.bt.pi.core.past.content.DhtContentHeader;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.past.content.KoalaMutableContent;
import com.bt.pi.core.past.content.KoalaPiEntityContent;
import com.bt.pi.core.pastry_override.PersistentStorage;

public class PersistentDhtStorage extends PersistentStorage {
    private static final String UNCHECKED = "unchecked";
    private static final String COLON = ":";
    private static final String READ_CONTENT_HEADERS_S = "readContentHeaders(%s)";
    private static final String UTF8 = "UTF8";
    private static final Log LOG = LogFactory.getLog(PersistentDhtStorage.class);

    protected PersistentDhtStorage() {
        super();
    }

    public PersistentDhtStorage(KoalaIdFactory factory, KoalaPiEntityFactory koalaPiEntityFactory, String name, String rootDir, long size, boolean index, Environment env, int metaDataSyncTime) throws IOException {
        super(factory, koalaPiEntityFactory, name, rootDir, size, index, env, metaDataSyncTime);
    }

    // for unit tests
    void setMetadataMap(ReverseTreeMap map) {
        metadata = map;
    }

    @Override
    protected Serializable readData(File file) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("readData(%s)", file.getAbsolutePath()));

        List<String> lines = readFile(file);
        Map<String, String> contentMetadata = readContentHeaders(lines);
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("Read content headers %s", contentMetadata));
        String content = readContent(lines);

        String idHeader = contentMetadata.get(DhtContentHeader.ID);
        if (idHeader == null)
            throw new NullPointerException(String.format("Null id header in dht content for file %s", file.getName()));
        Id id = rice.pastry.Id.build(idHeader);
        KoalaGCPastMetadata koalaGCPastMetadata = (KoalaGCPastMetadata) metadata.get(forDht(id));
        return new KoalaPiEntityContent(id, content, contentMetadata, koalaGCPastMetadata);
    }

    @SuppressWarnings(UNCHECKED)
    private List<String> readFile(File file) throws IOException {
        List<String> lines = FileUtils.readLines(file, UTF8);
        return lines;
    }

    private Map<String, String> readContentHeaders(List<String> lines) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(READ_CONTENT_HEADERS_S, lines));
        Map<String, String> result = new HashMap<String, String>();
        for (String line : lines) {
            if (line.trim().length() < 1)
                break;
            int colonPos = line.indexOf(COLON);
            if (colonPos < 0)
                throw new DhtContentPersistenceException(String.format("Invalid content header: %s", line));
            String header = line.split(COLON)[0].trim();
            String value = line.substring(colonPos + 1).trim();
            result.put(header, value);
        }
        return result;
    }

    private String readContent(List<String> lines) {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("readContent(%s)", lines));
        StringBuffer result = new StringBuffer();
        boolean foundEndOfHeaders = false;
        for (String line : lines) {
            if (line.trim().length() < 1) {
                foundEndOfHeaders = true;
                continue;
            }
            if (foundEndOfHeaders)
                result.append(line);
        }
        return result.toString();
    }

    protected Map<String, String> readContentHeaders(File file) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format(READ_CONTENT_HEADERS_S, file.getName()));
        List<String> lines = readFile(file);
        return readContentHeaders(lines);
    }

    @Override
    protected Id readKeyFromFile(File file) throws IOException {
        Map<String, String> contentHeaders = readContentHeaders(file);
        return rice.pastry.Id.build(contentHeaders.get(DhtContentHeader.ID));
    }

    @Override
    protected long readVersion(File file) throws IOException {
        Map<String, String> contentHeaders = readContentHeaders(file);
        return Long.parseLong(contentHeaders.get(DhtContentHeader.CONTENT_VERSION));
    }

    @Override
    protected long writeObject(Serializable obj, Serializable metadata, Id key, long version, File file) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("writeObject(%s, %s, %s, %s, %s)", obj, metadata, key, version, file));
        if (!(obj instanceof KoalaMutableContent))
            throw new DhtContentPersistenceException(String.format("NOT pi mutable content: %s", obj));

        KoalaMutableContent content = (KoalaMutableContent) obj;
        long contentVersion = content.getVersion();
        if (contentVersion < 0)
            throw new DhtContentPersistenceException(String.format("Content version (%d) not set!!", contentVersion));

        List<String> lines = new ArrayList<String>();
        Map<String, String> contentHeaders = content.getContentHeaders();
        for (Entry<String, String> entry : contentHeaders.entrySet())
            lines.add(entry.getKey() + ": " + entry.getValue());

        lines.add("");
        lines.add(content.getBody());

        FileUtils.writeLines(file, UTF8, lines);
        return file.length();
    }

    @Override
    protected Serializable readMetadata(File file) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug(String.format("readMetadata(%s)", file.getAbsolutePath()));

        long version = readVersion(file);
        List<String> lines = readFile(file);
        String json = readContent(lines);

        PiEntity piEntity = getKoalaPiEntityFactory().getPiEntity(json);

        // if the type is not recognised, i.e. old, then we set it to null and set the deletableAndDeleted flag to true
        // to force garbage collection
        String entityType = null;
        boolean isDeletedOrDeletable = true;
        if (null != piEntity) {
            entityType = piEntity.getType();
            isDeletedOrDeletable = isDeletedAndDeletable(piEntity);
        }

        return new KoalaGCPastMetadata(version, isDeletedOrDeletable, entityType);
    }

    private boolean isDeletedAndDeletable(PiEntity piEntity) {
        if (piEntity instanceof Deletable) {
            Deletable deletable = (Deletable) piEntity;
            return deletable.isDeleted();
        }
        return false;
    }

    @Override
    protected void writeMetadata(File file, Serializable metadata) throws IOException {
    }
}

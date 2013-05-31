package com.bt.pi.core.application.storage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import rice.Continuation;
import rice.p2p.commonapi.Id;
import rice.p2p.past.gc.GCId;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.storage.DhtContentPersistenceException;
import com.bt.pi.core.past.content.KoalaGCPastMetadata;
import com.bt.pi.core.past.content.KoalaMutableContent;

@Component
public class LocalStorageHousekeepingHandler implements LocalStorageScanningHandler {
    private static final Log LOG = LogFactory.getLog(LocalStorageHousekeepingHandler.class);
    private static final String DEFAULT_DHT_ARCHIVE_DIRECTORY = "var/storage_archive";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    @Resource
    private LocalStorageScanningApplication localStorageScanningApplication;
    private String archiveDirectory = DEFAULT_DHT_ARCHIVE_DIRECTORY;

    public LocalStorageHousekeepingHandler() {
        this.localStorageScanningApplication = null;
    }

    @Property(key = "local.storage.housekeeper.archive.directory", defaultValue = DEFAULT_DHT_ARCHIVE_DIRECTORY)
    public void setArchiveDirectory(String s) {
        this.archiveDirectory = s;
    }

    @Override
    public void handle(final Id id, KoalaGCPastMetadata metadata) {
        LOG.debug(String.format("handle(%s, %s)", id.toStringFull(), metadata));
        if (metadata.isDeletedAndDeletable()) {
            unstoreAndArchive(id);
        }
    }

    private void unstoreAndArchive(final Id id) {
        LOG.debug(String.format("unstoreAndArchive(%s)", id.toStringFull()));
        this.localStorageScanningApplication.getPersistentDhtStorage().getObject(id, new Continuation() {
            @Override
            public void receiveResult(Object result) {

                try {
                    createArchive(id, result);
                } catch (DhtContentPersistenceException e) {
                    LOG.warn(e);
                    return;
                }

                localStorageScanningApplication.getPersistentDhtStorage().unstore(id, new Continuation() {
                    @Override
                    public void receiveResult(Object result) {
                        boolean deleted = Boolean.TRUE.equals(result);
                        LOG.debug(String.format("id %s%s deleted from local storage", id.toStringFull(), deleted ? "" : " NOT"));
                    }

                    @Override
                    public void receiveException(Exception e) {
                        LOG.error(String.format("exception deleting %s from local storage", id.toStringFull()), e);
                    }
                });
            }

            @Override
            public void receiveException(Exception e) {
                LOG.error(String.format("exception reading %s from local storage", id.toStringFull()), e);
            }
        });
    }

    private void createArchive(Id id, Object obj) {
        LOG.debug(String.format("createArchive(%s, %s)", id.toStringFull(), obj));

        if (!(obj instanceof KoalaMutableContent))
            throw new DhtContentPersistenceException(String.format("NOT pi mutable content: %s", obj));

        KoalaMutableContent content = (KoalaMutableContent) obj;

        List<String> lines = new ArrayList<String>();
        Map<String, String> contentHeaders = content.getContentHeaders();
        for (Entry<String, String> entry : contentHeaders.entrySet())
            lines.add(entry.getKey() + ": " + entry.getValue());

        lines.add("");
        lines.add(content.getBody());

        // never sure when a GCId will turn up so just in case....
        String basename = id instanceof GCId ? ((GCId) id).getId().toStringFull() : id.toStringFull();

        String filename = String.format("%s/%s/%s/%s", this.archiveDirectory, this.localStorageScanningApplication.getNodeIdFull(), DATE_FORMAT.format(new Date()), basename);
        try {
            FileUtils.writeLines(new File(filename), "utf8", lines);
        } catch (IOException e) {
            LOG.error(String.format("error writing file %s", filename), e);
            throw new DhtContentPersistenceException(e.getMessage());
        }
    }
}

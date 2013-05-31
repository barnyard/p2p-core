package com.bt.pi.core.past.content;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import rice.p2p.commonapi.Id;
import rice.p2p.past.gc.GCPastMetadata;

import com.bt.pi.core.scope.NodeScope;

public class KoalaPiEntityContent extends KoalaMutableContent {
    private static final long serialVersionUID = 1L;

    private boolean deletedAndDeletable;
    private String entityType;
    private int backups;
    private NodeScope backupScope;
    private String entityUrl;

    private long version;

    public KoalaPiEntityContent(Id id, String entity, Map<String, String> metadata) {
        super(id, entity, metadata);
    }

    public KoalaPiEntityContent(Id id, String entity, Map<String, String> metadata, KoalaGCPastMetadata koalaGCPastMetadata) {
        super(id, entity, metadata);
        if (null != koalaGCPastMetadata) {
            this.deletedAndDeletable = koalaGCPastMetadata.isDeletedAndDeletable();
            this.entityType = koalaGCPastMetadata.getEntityType();
        }
    }

    public KoalaPiEntityContent(Id id, String json, boolean isDeletedAndDeletable, String anEntityType, int abackups, NodeScope abackupScope, String anEntityUrl, long entityVersion) {
        super(id, json, null);
        this.backups = abackups;
        this.backupScope = abackupScope;
        this.entityUrl = anEntityUrl;
        this.version = entityVersion;

        deletedAndDeletable = isDeletedAndDeletable;
        entityType = anEntityType;

        Map<String, String> contentMetadata = new HashMap<String, String>();
        contentMetadata.put(DhtContentHeader.ID, id.toStringFull());
        contentMetadata.put(DhtContentHeader.URI, anEntityUrl);
        contentMetadata.put(DhtContentHeader.CONTENT_VERSION, Long.toString(entityVersion));
        contentMetadata.put(DhtContentHeader.CONTENT_TYPE, DhtContentType.JSON_PI_ENTITY);
        contentMetadata.put(DhtContentHeader.BACKUP_SCOPE, abackupScope.toString());
        contentMetadata.put(DhtContentHeader.BACKUPS, String.valueOf(abackups));
        setContentHeaders(contentMetadata);
    }

    public boolean isDeletedAndDeletable() {
        return deletedAndDeletable;
    }

    public String getEntityType() {
        return entityType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KoalaPiEntityContent))
            return false;
        KoalaPiEntityContent castOther = (KoalaPiEntityContent) other;
        return new EqualsBuilder().appendSuper(super.equals(other)).append(deletedAndDeletable, castOther.deletedAndDeletable).append(entityType, castOther.entityType).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(deletedAndDeletable).append(entityType).toHashCode();
    }

    @Override
    public KoalaPiEntityContent duplicate() {
        return new KoalaPiEntityContent(this.getId(), this.getBody(), this.isDeletedAndDeletable(), this.entityType, this.backups, this.backupScope, this.entityUrl, this.version);
    }

    @Override
    public GCPastMetadata getMetadata(long expiration) {
        return new KoalaGCPastMetadata(expiration, deletedAndDeletable, entityType);
    }
}

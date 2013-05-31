package com.bt.pi.core.message.payload;

import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.stereotype.Component;

import com.bt.pi.core.entity.Backupable;
import com.bt.pi.core.entity.EntityScope;
import com.bt.pi.core.entity.PiEntityBase;
import com.bt.pi.core.scope.NodeScope;

@Component
@Backupable
@EntityScope(scope = NodeScope.REGION)
public class EchoPayload extends PiEntityBase {
    private String uuid;

    public EchoPayload() {
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public String getType() {
        return "EchoPayload";
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid
     *            the uuid to set
     */
    public void setUuid(String anId) {
        this.uuid = anId;
    }

    @Override
    public String getUrl() {
        return "echo:" + uuid;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof EchoPayload))
            return false;
        EchoPayload castOther = (EchoPayload) other;
        return new EqualsBuilder().append(uuid, castOther.uuid).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(uuid).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("uuid", uuid).toString();
    }

    @Override
    public String getUriScheme() {
        return "ep";
    }
}

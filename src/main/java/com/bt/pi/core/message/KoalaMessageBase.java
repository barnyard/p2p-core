//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.core.message;

import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.rawserialization.OutputBuffer;

import com.bt.pi.core.parser.KoalaJsonParser;

public abstract class KoalaMessageBase implements KoalaMessage {
    public static final String MESSAGE_TYPE_PARAM = "koalaMessageType";
    private static final String EQUALS = "=";
    private static final String UID = "uid";
    private static final Log LOG = LogFactory.getLog(KoalaMessageBase.class);
    private static final long serialVersionUID = 1L;
    private String uid;
    private String correlationUID;
    private String transactionUID;
    private String koalaMessageType;
    private long createdAt;
    private int destinationApplication;

    @JsonIgnore
    private transient KoalaJsonParser koalaJsonParser;

    /*
     * This constructor is advised not to be used as it does not use the pastry
     * Env. timesource.
     */
    public KoalaMessageBase(String aKoalaMessageType) {
        this(aKoalaMessageType, System.currentTimeMillis());
    }

    public KoalaMessageBase(String aKoalaMessageType, long messageCreatedAt) {
        this(-1, aKoalaMessageType, messageCreatedAt);
    }

    public KoalaMessageBase(int dest, String aKoalaMessageType, long messageCreatedAt) {
        uid = createUID();
        correlationUID = createUID();
        transactionUID = null;
        createdAt = messageCreatedAt;
        koalaMessageType = aKoalaMessageType;
        destinationApplication = dest;
        koalaJsonParser = null;
    }

    // this kinda makes me want to puke.
    // I will think on a better way of doing this.
    @Resource
    @JsonIgnore
    public void setKoalaJsonParser(KoalaJsonParser jsonParser) {
        koalaJsonParser = jsonParser;
    }

    @JsonIgnore
    public KoalaJsonParser getKoalaJsonParser() {
        return koalaJsonParser;
    }

    protected String createUID() {
        return UUID.randomUUID().toString();
    }

    public String getUID() {
        return uid;
    }

    public void setUID(String aUid) {
        uid = aUid;
    }

    public String getCorrelationUID() {
        return correlationUID;
    }

    public void setCorrelationUID(String aCorrelationUID) {
        correlationUID = aCorrelationUID;
    }

    public String getTransactionUID() {
        return transactionUID;
    }

    public void setTransactionUID(String aTransactionUID) {
        transactionUID = aTransactionUID;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long time) {
        createdAt = time;
    }

    public short getType() {
        return 1;
    }

    public int getPriority() {
        return Message.MEDIUM_PRIORITY;
    }

    public String getKoalaMessageType() {
        return koalaMessageType;
    }

    @Override
    public void serialize(OutputBuffer aOutputBuffer) {
        try {
            LOG.debug(String.format("serialize(OutputBuffer - %s)", aOutputBuffer));
            byte[] jsonString = koalaJsonParser.getJson(this).getBytes("UTF-8");

            int length = jsonString.length;
            aOutputBuffer.writeInt(length);
            aOutputBuffer.write(jsonString, 0, length);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            throw new RuntimeException(t);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(UID + EQUALS + uid).append(",correlationUID=" + correlationUID).append(",transactionUID=" + transactionUID).append("," + MESSAGE_TYPE_PARAM + EQUALS + koalaMessageType).append(",createdAt=" + createdAt)
                .append("destinationApplication=" + destinationApplication).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof KoalaMessageBase))
            return false;
        KoalaMessageBase castOther = (KoalaMessageBase) other;
        return new EqualsBuilder().append(uid, castOther.uid).append(correlationUID, castOther.correlationUID).append(transactionUID, castOther.transactionUID).append(koalaMessageType, castOther.koalaMessageType).append(createdAt,
                castOther.createdAt).append(destinationApplication, castOther.destinationApplication).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(uid).append(correlationUID).append(transactionUID).append(koalaMessageType).append(createdAt).append(destinationApplication).toHashCode();
    }

}

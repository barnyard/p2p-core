package com.bt.pi.core.past.message;

import java.io.IOException;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.past.PastContent;
import rice.p2p.past.messaging.InsertMessage;
import rice.p2p.past.rawserialization.PastContentDeserializer;

public class InsertRequestMessage extends InsertMessage {
    public static final short TYPE = 101;
    private static final long serialVersionUID = -7027957470028259605L;

    // the timestamp at which the object expires
    private long expiration;

    public InsertRequestMessage(int uid, PastContent content, long expirationDate, NodeHandle source, Id dest) {
        super(uid, content, source, dest);
        this.expiration = expirationDate;
    }

    protected InsertRequestMessage(InputBuffer buf, Endpoint endpoint, PastContentDeserializer pcd) throws IOException {
        super(buf, endpoint, pcd);
        expiration = buf.readLong();
    }

    public static InsertRequestMessage buildKoalaGC(InputBuffer buf, Endpoint endpoint, PastContentDeserializer pcd) throws IOException {
        byte version = buf.readByte();
        switch (version) {
        case 0:
            return new InsertRequestMessage(buf, endpoint, pcd);
        default:
            throw new IOException("Unknown Version: " + version);
        }
    }

    /**
     * Method which returns the expiration time
     * 
     * @return The contained expiration time
     */
    public long getExpiration() {
        return expiration;
    }

    protected void setExpiration(long expirtionDate) {
        expiration = expirtionDate;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    public void serialize(OutputBuffer buf) throws IOException {
        buf.writeByte((byte) 0); // version
        super.serializeHelper(buf);
        buf.writeLong(expiration);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[" + getClass() + "]").append(",UID= " + super.getUID()).append(",Source= " + super.getSource()).append(
                ",Destination= " + super.getDestination()).append(",expiration=" + expiration).append(",isResponse=" + isResponse).append(",response=" + response).append(
                ",exception=" + exception).toString();
    }
}

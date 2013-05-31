package com.bt.pi.core.past.message;

import java.io.IOException;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.past.messaging.ContinuationMessage;
import rice.p2p.past.rawserialization.PastContentHandleDeserializer;
import rice.p2p.past.rawserialization.RawPastContentHandle;

public class FetchBackupHandleMessage extends ContinuationMessage {
    public static final short TYPE = 103;
    private static final long serialVersionUID = 7101569231693538730L;

    // the id to fetch
    private Id id;

    public FetchBackupHandleMessage(int uid, Id idToFetch, NodeHandle source, Id dest) {
        super(uid, source, dest);
        id = idToFetch;
    }

    private FetchBackupHandleMessage(InputBuffer buf, Endpoint endpoint, PastContentHandleDeserializer pchd) throws IOException {
        super(buf, endpoint);
        // if called super.serializer(x, true) these will be set
        if (serType == S_SUB) {
            short type = buf.readShort();
            response = pchd.deserializePastContentHandle(buf, endpoint, type);
        }
        id = endpoint.readId(buf, buf.readShort());
    }

    public static FetchBackupHandleMessage build(InputBuffer buf, Endpoint endpoint, PastContentHandleDeserializer pchd) throws IOException {
        byte version = buf.readByte();
        switch (version) {
        case 0:
            return new FetchBackupHandleMessage(buf, endpoint, pchd);
        default:
            throw new IOException("Unknown Version: " + version);
        }
    }

    @Override
    public short getType() {
        return TYPE;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id anId) {
        this.id = anId;
    }

    public String toString() {
        return "[FetchBackupHandleMessage for " + id + "]";
    }

    @Override
    public void serialize(OutputBuffer buf) throws IOException {
        buf.writeByte((byte) 0); // version
        if (response != null && response instanceof RawPastContentHandle) {
            super.serialize(buf, false);
            RawPastContentHandle rpch = (RawPastContentHandle) response;
            buf.writeShort(rpch.getType());
            rpch.serialize(buf);
        } else {
            super.serialize(buf, true);
        }
        buf.writeShort(id.getType());
        id.serialize(buf);
    }
}

package com.bt.pi.core.past.message;

import java.io.IOException;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.past.rawserialization.JavaSerializedPastContent;
import rice.p2p.past.rawserialization.PastContentDeserializer;

import com.bt.pi.core.past.content.KoalaContentBase;

public class InsertBackupMessage extends InsertRequestMessage {
    public static final short TYPE = 102;
    private static final long serialVersionUID = -3688865701379393923L;

    public InsertBackupMessage(int uid, KoalaContentBase content, long expirationDate, NodeHandle source, Id dest) {
        super(uid, content, expirationDate, source, dest);
    }

    protected InsertBackupMessage(InputBuffer buf, Endpoint endpoint, PastContentDeserializer pcd) throws IOException {
        super(buf, endpoint, pcd);
    }

    public static InsertBackupMessage buildKoalaGC(InputBuffer buf, Endpoint endpoint, PastContentDeserializer pcd) throws IOException {
        byte version = buf.readByte();
        switch (version) {
        case 0:
            return new InsertBackupMessage(buf, endpoint, pcd);
        default:
            throw new IOException("Unknown Version: " + version);
        }
    }

    public KoalaContentBase getContent() {
        // if (content == null)
        if (content.getType() == 0)
            return (KoalaContentBase) ((JavaSerializedPastContent) content).getContent();
        return (KoalaContentBase) content;
    }

    @Override
    public short getType() {
        return TYPE;
    }
}

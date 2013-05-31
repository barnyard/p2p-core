package com.bt.pi.core.scribe.content;

import java.io.IOException;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.scribe.Topic;
import rice.p2p.scribe.messaging.ScribeMessage;

import com.bt.pi.core.entity.EntityMethod;

public class WrappedScribeContentMessage extends ScribeMessage {
    public static final short TYPE = 101;
    private static final String UTF_8 = "UTF-8";
    private static final long serialVersionUID = -3533550915976146934L;

    private WrappedScribeContentMessageType wrappedScribeContentMessageType;
    private String transactionUID;
    private EntityMethod entityMethod;
    private String jsonData;

    public WrappedScribeContentMessage() {
        this(null, null, null, null, null, null);
    }

    public WrappedScribeContentMessage(NodeHandle sourceHandle, Topic aTopic, WrappedScribeContentMessageType aWrappedScribeContentMessageType, EntityMethod anEntityMethod, String aJsonData, String aTransactionUID) {
        super(sourceHandle, aTopic);
        wrappedScribeContentMessageType = aWrappedScribeContentMessageType;
        entityMethod = anEntityMethod;
        jsonData = aJsonData;
        transactionUID = aTransactionUID;
    }

    protected WrappedScribeContentMessage(InputBuffer buf, Endpoint endpoint) throws IOException {
        super(buf, endpoint);
        deserializeThis(buf);
    }

    protected void deserializeThis(InputBuffer buf) throws IOException {
        wrappedScribeContentMessageType = WrappedScribeContentMessageType.valueOf(readUTF8StringWithLength(buf));
        transactionUID = readUTF8StringWithLength(buf);
        entityMethod = EntityMethod.valueOf(readUTF8StringWithLength(buf));
        jsonData = readUTF8StringWithLength(buf);
    }

    private String readUTF8StringWithLength(InputBuffer buf) throws IOException {
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.read(bytes);
        String res = new String(bytes, UTF_8);
        return res;
    }

    public WrappedScribeContentMessageType getWrappedScribeContentMessageType() {
        return wrappedScribeContentMessageType;
    }

    public EntityMethod getEntityMethod() {
        return entityMethod;
    }

    public String getJsonData() {
        return jsonData;
    }

    public String getTransactionUID() {
        return transactionUID;
    }

    @Override
    public void serialize(OutputBuffer buf) throws IOException {
        super.serialize(buf);
        serializeThis(buf);
    }

    protected void serializeThis(OutputBuffer buf) throws IOException {
        writeUTF8WithLengthPrefix(buf, wrappedScribeContentMessageType.toString());
        writeUTF8WithLengthPrefix(buf, transactionUID);
        writeUTF8WithLengthPrefix(buf, entityMethod.toString());
        writeUTF8WithLengthPrefix(buf, jsonData);
    }

    private void writeUTF8WithLengthPrefix(OutputBuffer buf, String str) throws IOException {
        byte[] utf8 = str.getBytes(UTF_8);
        buf.writeInt(utf8.length);
        buf.write(utf8, 0, utf8.length);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("wrappedScribeContentMessageType=" + wrappedScribeContentMessageType).append("jsonData=" + jsonData).toString();
    }

    @Override
    public short getType() {
        return TYPE;
    }

    public static Message build(InputBuffer buf, Endpoint endpoint) throws IOException {
        return new WrappedScribeContentMessage(buf, endpoint);
    }
}

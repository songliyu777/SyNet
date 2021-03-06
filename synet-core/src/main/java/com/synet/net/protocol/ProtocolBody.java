package com.synet.net.protocol;

import java.nio.ByteBuffer;

public class ProtocolBody {

    ByteBuffer byteBuffer = null;

    public ProtocolBody(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    /**
     * protobuf 数据
     */
    public void getProtobuf(byte[] protobuf) {
        byteBuffer.position(NetProtocol.protobuf_index);
        byteBuffer.get(protobuf);
    }

    public void setProtobuf(byte[] protobuf) {
        byteBuffer.position(NetProtocol.protobuf_index);
        byteBuffer.put(protobuf);
    }


}

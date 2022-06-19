package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;

public class InternalCodec implements ICodec {
    @Override
    public InternalMessage decode(Buffer buffer) {
        InternalMessage msg = new InternalMessage();
        msg.setId(buffer.getLong(0));
        msg.setData(buffer.getBytes(8, buffer.length()));
        return msg;
    }

    @Override
    public Buffer encode(InternalMessage message) {
        byte[] data = message.getData();
        Buffer buffer = Buffer.buffer(data.length + 8)
                .setLong(0, message.getId())
                .setBytes(8, data);
        return buffer;
    }
}

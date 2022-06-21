package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;

public class ClientCodec implements ICodec {
    private final Base64Codec inbound = new Base64Codec();
    private final Base64SwappedCodec outbound = new Base64SwappedCodec();

    @Override
    public InternalMessage decode(Buffer buffer) {
        return inbound.decode(buffer);
    }

    @Override
    public Buffer encode(InternalMessage message) {
        return outbound.encode(message);
    }
}

package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;

public class ServerCodec implements ICodec {
    private final Base64Codec outbound = new Base64Codec();
    private final Base64SwappedCodec inbound = new Base64SwappedCodec();

    @Override
    public InternalMessage decode(Buffer buffer) {
        return inbound.decode(buffer);
    }

    @Override
    public Buffer encode(InternalMessage message) {
        return outbound.encode(message);
    }
}

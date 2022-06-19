package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;

public interface ICodec {
    public abstract InternalMessage decode(Buffer buffer);

    public abstract Buffer encode(InternalMessage message);
}

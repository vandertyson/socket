package com.battle.model;

import com.battle.codec.InternalCodec;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalCodecTest {
    @Test
    public void testEncodeDecode() {
        String payload = "Hello putang ina mo";
        InternalCodec internalCodec = new InternalCodec();
        InternalMessage internalMessage = new InternalMessage(payload.getBytes(), 1l);
        Buffer encode = internalCodec.encode(internalMessage);
        InternalMessage decode = internalCodec.decode(encode);
        String s = new String(decode.getData(), CharsetUtil.UTF_8);
        assertEquals(s, payload);
        assertEquals(decode.getId(), internalMessage.getId());
    }
}
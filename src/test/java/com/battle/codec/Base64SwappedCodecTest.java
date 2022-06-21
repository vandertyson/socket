package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base64SwappedCodecTest {
    @Test
    void testEncodeDecode() {
        String data = "{\"event\":\"walk\",\"data\":{\"gps\":[110000,110000],\"grav\":[12,212,122],\"step\":111}}";
        InternalMessage msg = new InternalMessage(data);
        Base64SwappedCodec codec = new Base64SwappedCodec();
        Buffer encode = codec.encode(msg);
        InternalMessage decode = codec.decode(encode);
        assertEquals(data, decode.getData());
    }
}
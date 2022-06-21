package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

public class Base64Codec implements ICodec {
    private static final Logger logger = LogManager.getLogger(Base64Codec.class);
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public InternalMessage decode(Buffer buffer) {
        String base64 = new String(buffer.getBytes());
        byte[] data = decoder.decode(base64);
        String decode = new String(data);
        if (logger.isDebugEnabled()) {
            logger.debug("Buffer.decode  base64={} decode={}", base64, decode);
        }
        return new InternalMessage(decode);
    }

    @Override
    public Buffer encode(InternalMessage message) {
        String baseString = message.getData();
        String data = encoder.encodeToString(baseString.getBytes());
        if (logger.isDebugEnabled()) {
            logger.debug("Buffer.encode baseString={} base64={}", baseString, data);
        }
        return Buffer.buffer(data);
    }

    public static void main(String[] args) {

    }
}

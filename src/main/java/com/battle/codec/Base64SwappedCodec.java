package com.battle.codec;

import com.battle.model.InternalMessage;
import io.vertx.core.buffer.Buffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Base64SwappedCodec implements ICodec {
    private static final Logger logger = LogManager.getLogger(Base64SwappedCodec.class);
    private static final String splitPattern = "(?<=\\G.{8})";
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();

    @Override
    public InternalMessage decode(Buffer buffer) {
        String swapped = new String(buffer.getBytes());
        String[] split = swapped.split(splitPattern);
        List<String> strings = Arrays.asList(String.valueOf(swapped.length()).split(""));
        Collections.reverse(strings);
        String[] pattern = strings.toArray(String[]::new);
        String base64 = Arrays.asList(split).stream().map(f -> swap(pattern, f)).collect(Collectors.joining());
        byte[] data = decoder.decode(base64);
        String decode = new String(data);
        if (logger.isDebugEnabled()) {
            logger.debug("Buffer.decode baseString={} base64={} decode={}", swapped, base64, decode);
        }
        return new InternalMessage(decode);
    }

    @Override
    public Buffer encode(InternalMessage message) {
        String baseString = message.getData();
        String data = encoder.encodeToString(baseString.getBytes());
        String[] pattern = String.valueOf(data.length()).split("");
        String[] split = data.split(splitPattern);
        String collect = Arrays.asList(split).stream().map(f -> swap(pattern, f)).collect(Collectors.joining());
        if (logger.isDebugEnabled()) {
            logger.debug("Buffer.encode baseString={} base64={} encode={}", baseString, data, collect);
        }
        return Buffer.buffer(collect);
    }

    private String swap(String[] pattern, String input) {
        String[] split = input.split("");
        for (int i = 0; i < pattern.length; i++) {
            if (i + 1 >= pattern.length) {
                break;
            }
            Integer i1 = Integer.valueOf(pattern[i]);
            Integer i2 = Integer.valueOf(pattern[i + 1]);
            String tmp = split[i1];
            split[i1] = split[i2];
            split[i2] = tmp;
        }
        return Arrays.stream(split).collect(Collectors.joining());
    }
}

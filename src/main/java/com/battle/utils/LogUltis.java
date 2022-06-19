package com.battle.utils;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class LogUltis {
    public static String toString(Object obj) {
        if (obj instanceof NetSocket) {
            return ((NetSocket) obj).remoteAddress().toString();
        }
        if (obj instanceof Buffer) {
            Buffer bb = (Buffer) obj;
            return "{size=" + bb.length() + ", hex={" + ByteBufUtil.hexDump(bb.getBytes()) + "}";
        }
        return obj.toString();
    }
}

package com.battle.server;

import com.battle.codec.ICodec;
import com.battle.model.ISenderInfo;
import com.battle.model.IServerContext;
import com.battle.model.InternalMessage;
import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class ServerContextImpl implements IServerContext {
    private static final Logger logger = LogManager.getLogger(ServerContextImpl.class);
    private final Buffer requestBuffer;
    private final NetSocket socket;
    private final ICodec codec;

    private final ISenderInfo senderInfo;

    private final InternalMessage request;

    @Override
    public ISenderInfo getSenderInfo() {
        return senderInfo;
    }

    @Override
    public void sendResponse(byte[] data) {
        InternalMessage internalMessage = new InternalMessage(data, request.getId());
        Buffer encode = codec.encode(internalMessage);
        socket.write(encode);
    }

    @Override
    public void sendResponse(String data) {
        sendResponse(data.getBytes(CharsetUtil.UTF_8));
    }

    public ServerContextImpl(Buffer requestBuffer, NetSocket socket, ICodec codec) {
        Objects.requireNonNull(requestBuffer);
        Objects.requireNonNull(socket);
        Objects.requireNonNull(codec);
        this.requestBuffer = requestBuffer;
        this.socket = socket;
        this.codec = codec;
        this.request = codec.decode(requestBuffer);
        this.senderInfo = new ISenderInfo() {
            @Override
            public String getHost() {
                return socket.remoteAddress().host();
            }

            @Override
            public int getPort() {
                return socket.remoteAddress().port();
            }
        };
    }

    @Override
    public InternalMessage getRequest() {
        return request;
    }
}

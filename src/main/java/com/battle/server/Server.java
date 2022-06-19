package com.battle.server;

import com.battle.codec.ICodec;
import com.battle.codec.InternalCodec;
import com.battle.model.IServerContext;
import com.battle.utils.LogUltis;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);
    private final Handler<IServerContext> handler;
    private final Vertx vertx;
    private final NetServerOptions options;
    private final NetServer server;
    private final ServerConfiguration configuration;
    private final Map<String, NetSocket> connectedSocket = new ConcurrentHashMap<>();
    private ICodec codec = new InternalCodec();

    public Server(ServerConfiguration configuration, Handler<IServerContext> handler) {
        Objects.requireNonNull(handler, "Handler is required");
        this.configuration = configuration;
        this.handler = handler;
        vertx = Vertx.vertx();
        options = new NetServerOptions().setPort(configuration.getPort());
        server = vertx.createNetServer(options);
    }

    public boolean start() throws ExecutionException, InterruptedException {
        CompletableFuture<Boolean> ret = new CompletableFuture();

        server.connectHandler(socket -> {
            String socketKey = getSocketKey(socket);
            connectedSocket.put(socketKey, socket);
            logger.info("new socket connected {}", socketKey);
            socket.handler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer event) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("inbound socket={} data={}}", LogUltis.toString(socket), LogUltis.toString(event));
                        }
                        handler.handle(new ServerContextImpl(event, socket, codec));
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            });
        });
        server.listen();
        logger.info("server is now listening on actual port: {}", server.actualPort());
        return ret.get();
    }

    private String getSocketKey(NetSocket socket) {
        return socket.remoteAddress().host() + ":" + socket.remoteAddress().port();
    }

}

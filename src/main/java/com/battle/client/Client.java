package com.battle.client;

import com.battle.codec.ClientCodec;
import com.battle.codec.ICodec;
import com.battle.model.IClientHandler;
import com.battle.model.ISenderInfo;
import com.battle.model.InternalMessage;
import com.battle.utils.LogUltis;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


public class Client {
    private static final Logger logger = LogManager.getLogger(Client.class);

    private final ClientConfiguration configuration;
    private final IClientHandler handler;
    private final Vertx vertx;
    private final NetClient client;

    private final ICodec codec = new ClientCodec();

    public Client(ClientConfiguration configuration, IClientHandler handler) {
        this(Vertx.vertx(), configuration, handler);
    }

    public Client(Vertx vertx, ClientConfiguration configuration, IClientHandler handler) {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(handler);
        this.configuration = configuration;
        this.handler = handler;
        this.vertx = vertx;
        NetClientOptions options = new NetClientOptions();
        client = vertx.createNetClient(options);
    }

    private class RoundRobinSelector<E> {
        final List<E> selections;

        public List<E> getSelections() {
            return selections;
        }

        public RoundRobinSelector(List<E> selections) {
            this.selections = selections;
        }

        private final AtomicLong counter = new AtomicLong();

        private E select() {
            long l = counter.incrementAndGet();
            if (l >= Long.MAX_VALUE - 100000) {
                counter.set(0);
            }
            int i = (int) (l % selections.size());
            return selections.get(i);
        }
    }

    private class SocketWrapper implements ISenderInfo {
        private final NetSocket socket;
        private final int port;
        private final String host;

        public NetSocket getSocket() {
            return socket;
        }

        public SocketWrapper(NetSocket socket) {
            Objects.requireNonNull(socket);
            this.socket = socket;
            this.host = socket.remoteAddress().host();
            this.port = socket.remoteAddress().port();
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "SocketWrapper{" +
                    "port=" + port +
                    ", host='" + host + '\'' +
                    '}';
        }
    }

    Map<String, RoundRobinSelector<SocketWrapper>> mapConnections = new ConcurrentHashMap<>();

    public void send(String host, Integer port, InternalMessage message) throws Exception {
        String key = getKey(host, port);
        RoundRobinSelector<SocketWrapper> selector = mapConnections.get(key);
        Buffer encode = codec.encode(message);
        if (selector != null) {
            SocketWrapper select = selector.select();
            select.getSocket().write(encode);
        } else {
            CompletableFuture<Boolean> connected = new CompletableFuture();
            RoundRobinSelector<SocketWrapper> sel = new RoundRobinSelector<SocketWrapper>(new CopyOnWriteArrayList<>());
            mapConnections.put(key, sel);
            for (int i = 0; i < configuration.getPoolSize(); i++) {
                client.connect(port, host, new Handler<AsyncResult<NetSocket>>() {
                    @Override
                    public void handle(AsyncResult<NetSocket> event) {
                        NetSocket socket = event.result();
                        SocketWrapper socketWrapper = new SocketWrapper(socket);
                        sel.getSelections().add(socketWrapper);
                        logger.info("new socket opened socket={} size={}", LogUltis.toString(socket), sel.getSelections().size());
                        socket.handler(new Handler<Buffer>() {
                            @Override
                            public void handle(Buffer event) {
                                try {
                                    InternalMessage msg = codec.decode(event);
                                    handler.onResponse(msg, socketWrapper);
                                } catch (Exception ex) {
                                    logger.error(ex, ex);
                                }
                            }
                        });
                        connected.complete(true);
                    }
                });
            }
            long l = System.currentTimeMillis();
            while (!connected.get()) {
                Thread.sleep(1);
                if (System.currentTimeMillis() - l > configuration.getMaxConnectTimeout()) {
                    throw new TimeoutException("Can not connect to server after " + configuration.getMaxConnectTimeout() + " ms");
                }
            }
            SocketWrapper select = mapConnections.get(key).select();
            select.getSocket().write(encode);
        }
    }

    private String getKey(String host, Integer port) {
        return host + ":" + port;
    }
}

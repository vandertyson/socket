package com.battle.server;

import com.battle.model.IMessage;
import io.vertx.core.Handler;

public class Server<E extends IMessage> {
    private final int port;
    private final Handler<E> handler;

    public Server(int port, Handler<E> handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start(){

    }

}

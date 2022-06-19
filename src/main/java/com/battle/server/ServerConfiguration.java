package com.battle.server;

public class ServerConfiguration {
    public ServerConfiguration() {
    }

    private int port = Integer.getInteger("server.port", 9000);

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

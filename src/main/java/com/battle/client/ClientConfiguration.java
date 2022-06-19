package com.battle.client;

public class ClientConfiguration {
    private long maxConnectTimeout = Long.getLong("client.maxConnectTimeout", 10000);
    private int poolSize = Integer.getInteger("client.poolSize", 10);

    public ClientConfiguration() {
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public long getMaxConnectTimeout() {
        return maxConnectTimeout;
    }

    public void setMaxConnectTimeout(long maxConnectTimeout) {
        this.maxConnectTimeout = maxConnectTimeout;
    }
}

package com.battle.app;

public class AppConfiguration {
    private long maxEnqueueTryMs = Long.getLong("app.maxEnqueueTryMs", 1000);

    private Integer queueMessageSize = Integer.getInteger("app.queueMessageSize", 1000);
    private String threadName = System.getProperty("app.threadName", "battle-");
    private int numThread = Integer.getInteger("app.numThread", 1000);
    private long emptyQueuePark = Long.getLong("app.emptyQueuePark", 100_000);
    private MongoConfiguration mongoConfig = new MongoConfiguration();

    public MongoConfiguration getMongoConfig() {
        return mongoConfig;
    }

    public void setMongoConfig(MongoConfiguration mongoConfig) {
        this.mongoConfig = mongoConfig;
    }

    public Integer getQueueMessageSize() {
        return queueMessageSize;
    }

    public void setQueueMessageSize(Integer queueMessageSize) {
        this.queueMessageSize = queueMessageSize;
    }

    public long getMaxEnqueueTryMs() {
        return maxEnqueueTryMs;
    }

    public void setMaxEnqueueTryMs(long maxEnqueueTryMs) {
        this.maxEnqueueTryMs = maxEnqueueTryMs;
    }

    public AppConfiguration() {
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getNumThread() {
        return numThread;
    }

    public void setNumThread(int numThread) {
        this.numThread = numThread;
    }

    public long getEmptyQueuePark() {
        return emptyQueuePark;
    }

    public void setEmptyQueuePark(long emptyQueuePark) {
        this.emptyQueuePark = emptyQueuePark;
    }
}

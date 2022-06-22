package com.battle.app;

import com.battle.model.InternalMessage;
import com.battle.server.Server;
import com.battle.utils.MetricsBenchmark;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

public class ServerProcessor implements Runnable {
    private static final Logger logger = LogManager.getLogger(ServerProcessor.class);
    private static final MetricsBenchmark mb = new MetricsBenchmark();
    private final Server server;
    private final Queue<MessageWrapper> queueMsg;
    private final IJsonService jsonService;
    private final AppConfiguration configuration;
    private boolean isRunning;

    private final MongoClient client;


    public ServerProcessor(Server server,
                           Queue<MessageWrapper> queueMsg,
                           IJsonService jsonService,
                           AppConfiguration configuration,
                           MongoClient client) {
        this.server = server;
        this.queueMsg = queueMsg;
        this.jsonService = jsonService;
        this.configuration = configuration;
        this.client = client;
        isRunning = true;
    }

    @Override
    public void run() {
        logger.info("Start dequeue");
        while (isRunning) {
            try {
                MessageWrapper poll = queueMsg.poll();
                if (poll != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Dequeue msg {} ", poll);
                    }
                    InternalMessage request = poll.getContext().getRequest();
                    String data = request.getData();
                    JsonObject entries = new JsonObject(data);
                    long start = System.nanoTime();
                    client.insert("data", entries, new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> res) {
                            if (res.succeeded()) {
                                String id = res.result();
                                logger.info("Inserted book with id " + id);
                                mb.statisticMetris(start, entries.size(), "Insert");
                            } else {
                                logger.error(res.cause(), res.cause());
                            }
                        }
                    });
                } else {
                    LockSupport.parkNanos(configuration.getEmptyQueuePark());
                }
            } catch (Exception ex) {
                logger.error(ex, ex);
            }
        }
    }

    public void shutDown() {
        isRunning = false;
    }
}

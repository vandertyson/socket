package com.battle.app;

import com.battle.client.Client;
import com.battle.client.ClientConfiguration;
import com.battle.model.IClientHandler;
import com.battle.model.ISenderInfo;
import com.battle.model.InternalMessage;
import com.battle.utils.MetricsBenchmark;
import com.battle.utils.YamlUtils;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jctools.queues.MpmcArrayQueue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class BenchmarkClient {
    private static final Logger logger = LogManager.getLogger(BenchmarkClient.class);
    private static final Integer concurrency = Integer.getInteger("concurrency", 10);
    private static final Integer numUser = Integer.getInteger("numUser", 10000);
    private static final Integer[] update = new Integer[]{2, 6, 2};
    private static final String host = System.getProperty("host", "localhost");
    private static final Integer port = Integer.getInteger("port", 9000);
    private static final IJsonService json = new GsonService();
    private static final MpmcArrayQueue<SampleRequest> queueRequest = new MpmcArrayQueue<>(10000);
    private static final MpmcArrayQueue<SampleResponse> queueResponse = new MpmcArrayQueue<>(10000);
    private static final Map<String, SampleResponse> mapCounter = new ConcurrentHashMap<>();
    private static final Map<Long, SentInfo> mapWait = new ConcurrentHashMap<>();
    private static final MetricsBenchmark mbDeser = new MetricsBenchmark();
    private static final MetricsBenchmark mbSer = new MetricsBenchmark();
    private static final Map<Integer, MetricsBenchmark> mapMb = new HashMap<>();

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        String log4jFile = System.getProperty("log4j.configurationFile");
        if (log4jFile == null || !(new File(log4jFile)).exists()) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            File log4j = new File("./etc/log4j2.xml");
            ctx.setConfigLocation(log4j.toURI());
        }
        ClientConfiguration configuration = new ClientConfiguration();
        YamlUtils.objectToYamlFile(configuration, "etc/clientConf.yml");
        Client client = new Client(configuration, new IClientHandler() {
            @Override
            public void onResponse(InternalMessage message, ISenderInfo sender) {
                long receive = System.currentTimeMillis();
                JsonObject obj = new JsonObject(message.getData());
                int length = message.getData().getBytes().length;
                mbDeser.statisticMetris(receive, length, "Deserialize");
                Long requestId = obj.getLong("requestId");
                SampleResponse response = new SampleResponse(requestId, obj.getString("userID"));
                SentInfo sentInfo = mapWait.remove(requestId);
                if (sentInfo != null) {
                    sentInfo.getWait().complete(true);
                    Integer requestType = sentInfo.getRequestType();
                    if (!mapMb.containsKey(requestType)) {
                        mapMb.put(requestType, new MetricsBenchmark());
                    }
                    mapMb.get(requestType).statisticMetris(sentInfo.getSentTime(), length, getRequestTypeString(requestType));
                } else {
                    logger.warn("No sent info for request {} ", requestId);
                }
                boolean offer = false;
                do {
                    offer = queueResponse.offer(response);
                    if (!offer) {
                        LockSupport.parkNanos(10000);
                    }
                } while (!offer);
            }
        });
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
//                Map<String, >
                for (int i = 0; i < numUser; i++) {

                }
                while (true) {
                    try {
                        SampleResponse poll = queueResponse.poll();
                        if (poll != null) {
                            poll.getRequestId();
                        } else {
                            LockSupport.parkNanos(10000);
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            }
        });

        for (int i = 0; i < concurrency; i++) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    logger.info("Start send");
                    while (true) {
                        SampleRequest poll = queueRequest.poll();
                        if (poll != null) {
                            String s = JsonObject.mapFrom(poll).toString();
                            SentInfo sentInfo = new SentInfo(poll.getType(), poll.getRequestID());
                            mapWait.put(poll.getRequestID(), sentInfo);
                            sentInfo.setSentTime(System.nanoTime());
                            try {
                                client.send(host, port, new InternalMessage(s));
                            } catch (Exception e) {
                                logger.error(e, e);
                            }
                            try {
                                sentInfo.getWait().get();
                            } catch (InterruptedException e) {
                                logger.error(e, e);
                            } catch (ExecutionException e) {
                                logger.error(e, e);
                            }
                        } else {
                            LockSupport.parkNanos(100000);
                        }
                    }
                }
            });
        }
    }

    private static String getRequestTypeString(Integer requestType) {
        if (requestType == 1) {
            return "Init";
        }
        if (requestType == 2) {
            return "Update";
        }
        if (requestType == 3) {
            return "Delete";
        }
        return "Unknown";
    }

    private static class SentInfo {
        private final Integer requestType;
        private final Long requestID;
        private final CompletableFuture<Boolean> wait = new CompletableFuture();

        private long sentTime;

        public long getSentTime() {
            return sentTime;
        }

        public void setSentTime(long sentTime) {
            this.sentTime = sentTime;
        }

        public SentInfo(Integer requestType, Long requestID) {
            this.requestType = requestType;
            this.requestID = requestID;
        }

        public Integer getRequestType() {
            return requestType;
        }

        public Long getRequestID() {
            return requestID;
        }

        public CompletableFuture<Boolean> getWait() {
            return wait;
        }
    }

    private static class Counter {
        private final AtomicLong init = new AtomicLong();
        private final AtomicLong update = new AtomicLong();
        private final AtomicLong delete = new AtomicLong();

        public AtomicLong getInit() {
            return init;
        }

        public AtomicLong getUpdate() {
            return update;
        }

        public AtomicLong getDelete() {
            return delete;
        }
    }

}

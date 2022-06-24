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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jctools.queues.MpmcArrayQueue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class BenchmarkClient {
    private static final Logger logger = LogManager.getLogger(BenchmarkClient.class);
    private static final Integer concurrency = Integer.getInteger("concurrency", 10);
    private static final Integer numUser = Integer.getInteger("numUser", 1000);
    private static final Integer[] update = new Integer[]{2, 6, 2};
    private static final String host = System.getProperty("host", "localhost");
    private static final Integer port = Integer.getInteger("port", 9000);
    private static final MpmcArrayQueue<Pair<User, SampleRequest>> queueRequest = new MpmcArrayQueue<>(10000);
    private static final MpmcArrayQueue<SampleResponse> queueResponse = new MpmcArrayQueue<>(10000);
    private static final Map<String, SampleResponse> mapCounter = new ConcurrentHashMap<>();
    private static final Map<Long, SentInfo> mapWait = new ConcurrentHashMap<>();
    private static final MetricsBenchmark mbDeser = new MetricsBenchmark();
    private static final MetricsBenchmark mbSer = new MetricsBenchmark();
    private static final Map<Integer, MetricsBenchmark> mapMb = new HashMap<>();
    private static final Integer maxUpdate = Integer.getInteger("maxUpdate", 10);
    private static final MpmcArrayQueue<User> qUser = new MpmcArrayQueue<>(numUser);

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        String log4jFile = System.getProperty("log4j.configurationFile");
        if (log4jFile == null || !(new File(log4jFile)).exists()) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            File log4j = new File("./etc/log4j2.xml");
            ctx.setConfigLocation(log4j.toURI());
        }
        String payload = "{\"event\":\"walk\",\"data\":{\"gps\":[110000,110000],\"grav\":[12,212,122],\"step\":111}}";
        JsonObject data = new JsonObject(payload);
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
                SentInfo sentInfo = mapWait.get(requestId);
                if (sentInfo != null) {
                    sentInfo.getWait().complete(true);
                    Integer requestType = getRequestTypeFromUser(sentInfo.getUser());
                    SampleResponse response = new SampleResponse(requestId, obj.getString("userID"), requestType);
                    if (!mapMb.containsKey(requestType)) {
                        mapMb.put(requestType, new MetricsBenchmark());
                    }
                    mapMb.get(requestType).statisticMetris(sentInfo.getSentTime(), length, getRequestTypeString(requestType));
                    doEnqueue(response, queueResponse);
                } else {
                    logger.warn("No sent info for request {} ", requestId);
                }
            }
        });


        for (int i = 0; i < numUser; i++) {
            qUser.offer(new User());
        }
        Thread threadInit = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        User user = qUser.poll();
                        if (user != null) {
                            SampleRequest sampleRequest = new SampleRequest(user.getUserId(), 1);
                            doEnqueue(new ImmutablePair<>(user, sampleRequest), queueRequest);
                        } else {
                            LockSupport.parkNanos(10000);
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            }
        });
        threadInit.setName("init-request");
        threadInit.start();

        Thread tResponse = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        SampleResponse poll = queueResponse.poll();
                        if (poll != null) {
                            Long requestId = poll.getRequestId();
                            String userId = poll.getUserId();
                            Integer requestType = poll.getRequestType();
                            SentInfo sentInfo = mapWait.remove(requestId);
                            //init thi gui update
                            if (sentInfo == null) {
                                logger.error("no sent info for request {}", requestId);
                                continue;
                            }
                            User user = sentInfo.getUser();
                            if (requestType == 1) {
                                SampleRequest rq = new SampleRequest(userId, 2);
                                rq.getTransactions().put(System.currentTimeMillis(), data);
                                doEnqueue(new ImmutablePair<>(user, rq), queueRequest);
                            } else if (requestType == 2) {
                                int i = sentInfo.getUser().getCountUpdate().get();
                                int type = i < maxUpdate ? 2 : 3;
                                SampleRequest rq = new SampleRequest(userId, type);
                                rq.getTransactions().put(System.currentTimeMillis(), data);
                                doEnqueue(new ImmutablePair<>(user, rq), queueRequest);
                            } else {
                                doEnqueue(new User(), qUser);
                            }
                        } else {
                            LockSupport.parkNanos(10000);
                        }
                    } catch (Exception ex) {
                        logger.error(ex, ex);
                    }
                }
            }
        });
        tResponse.setName("response-handler");
        tResponse.start();

        for (int i = 0; i < concurrency; i++) {
            Thread tSend = new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("Start send");
                    while (true) {
                        Pair<User, SampleRequest> poll = queueRequest.poll();
                        if (poll != null) {
                            long l = System.nanoTime();
                            SampleRequest request = poll.getValue();
                            User user = poll.getKey();
                            String s = JsonObject.mapFrom(request).toString();
                            mbSer.statisticMetris(l, s.getBytes().length, "Serialize");
                            SentInfo sentInfo = new SentInfo(user, request.getRequestID());
                            mapWait.put(request.getRequestID(), sentInfo);
                            sentInfo.setSentTime(System.nanoTime());
                            try {
                                client.send(host, port, new InternalMessage(s));
                                sentInfo.getWait().get();
                            } catch (Exception e) {
                                logger.error(e, e);
                                doEnqueue(poll, queueRequest);
                            }
                        } else {
                            LockSupport.parkNanos(100000);
                        }
                    }
                }
            });
            tSend.setName("send-" + i);
            tSend.start();
        }
    }

    private static Integer getRequestTypeFromUser(User user) {
        AtomicInteger countUpdate = user.getCountUpdate();
        if (countUpdate.get() == 0) {
            return 1;
        }
        if (countUpdate.get() < maxUpdate) {
            return 2;
        }
        return 3;
    }

    private static <T> void doEnqueue(T obj, MpmcArrayQueue<T> queue) {
        boolean offer = false;
        do {
            offer = queue.offer(obj);
            if (!offer) {
                LockSupport.parkNanos(10000);
            }
        } while (!offer);
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

    private static class User {
        private final String userId = UUID.randomUUID().toString();
        private final AtomicInteger countUpdate = new AtomicInteger();

        public User() {
        }

        public String getUserId() {
            return userId;
        }

        public AtomicInteger getCountUpdate() {
            return countUpdate;
        }
    }

    private static class SentInfo {
        private final User user;
        private final Long requestID;
        private final CompletableFuture<Boolean> wait = new CompletableFuture();

        private long sentTime;

        public long getSentTime() {
            return sentTime;
        }

        public void setSentTime(long sentTime) {
            this.sentTime = sentTime;
        }

        public SentInfo(User user, Long requestID) {
            this.user = user;
            this.requestID = requestID;
        }

        public User getUser() {
            return user;
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

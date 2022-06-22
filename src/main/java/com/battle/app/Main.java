package com.battle.app;

import com.battle.model.IServerContext;
import com.battle.server.Server;
import com.battle.server.ServerConfiguration;
import com.battle.utils.YamlUtils;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jctools.queues.MpmcArrayQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class);
    private static final String configPath = System.getProperty("configPath", "./etc/appConf.yml");
    private static final String serverConfigPath = System.getProperty("serverConfigPath", "./etc/serverConf.yml");

    public static void main(String[] args) throws Exception {

        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        String log4jFile = System.getProperty("log4j.configurationFile");
        if (log4jFile == null || !(new File(log4jFile)).exists()) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            File log4j = new File("./etc/log4j2.xml");
            ctx.setConfigLocation(log4j.toURI());
        }

        Vertx vertx = Vertx.vertx();
        IJsonService jsonService = new GsonService();
        AppConfiguration configuration = YamlUtils.objectFromYaml(AppConfiguration.class, configPath);
        logger.info("Application configuration loaded from {} \n {}", configPath, YamlUtils.objectToPrettyYaml(configuration));
        ServerConfiguration serverConfiguration = YamlUtils.objectFromYaml(ServerConfiguration.class, configPath);
        logger.info("Server configuration loaded from {} \n {}", serverConfigPath, YamlUtils.objectToPrettyYaml(serverConfiguration));
        MpmcArrayQueue<MessageWrapper> queueMsg = new MpmcArrayQueue<>(configuration.getQueueMessageSize());
        Server server = new Server(vertx, serverConfiguration, new Handler<IServerContext>() {
            @Override
            public void handle(IServerContext event) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Receive request. client={}, request={}", event.getSenderInfo(), event.getRequest());
                    }
                    MessageWrapper messageWrapper = new MessageWrapper(event);
                    boolean enqueue = false;
                    long l = System.currentTimeMillis();
                    do {
                        if (System.currentTimeMillis() - l > configuration.getMaxEnqueueTryMs()) {
                            ErrorMessage errorMessage = new ErrorMessage(null, ErrorMessage.ErrorCode.FULL_QUEUE);
                            String serialize = jsonService.serialize(errorMessage);
                            event.sendResponse(serialize);
                            break;
                        }
                        enqueue = queueMsg.offer(messageWrapper);
                    } while (!enqueue);
                } catch (Exception ex) {
                    logger.error(ex, ex);
                    ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(), ErrorMessage.ErrorCode.EXCEPTION);
                    String serialize = jsonService.serialize(errorMessage);
                    event.sendResponse(serialize);
                }
            }
        });
        boolean start = server.start();
        if (start) {
            logger.info("Server started at {}", serverConfiguration.getPort());
        }
        JsonObject mongoConfiguration = JsonObject.mapFrom(configuration.getMongoConfig());
        MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfiguration);

        AffinityThreadFactory atf = new AffinityThreadFactory(configuration.getThreadName(), true, AffinityStrategies.DIFFERENT_CORE);
        List<ServerProcessor> listProcessor = new ArrayList<>();
        List<Thread> listThread = new ArrayList<>();
        for (int i = 0; i < configuration.getNumThread(); i++) {
            ServerProcessor serverProcessor = new ServerProcessor(server, queueMsg, jsonService, configuration, mongoClient);
            Thread thread = atf.newThread(serverProcessor);
            listProcessor.add(serverProcessor);
            listThread.add(thread);
            thread.start();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                server.shutdown();
                for (ServerProcessor serverProcessor : listProcessor) {
                    serverProcessor.shutDown();
                }
            }
        }));
    }
}

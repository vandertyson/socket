package com.battle.server;

import com.battle.model.IServerContext;
import com.battle.model.InternalMessage;
import com.battle.utils.YamlUtils;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

public class SimpleServer {
    private static Logger logger = LogManager.getLogger(SimpleServer.class);

    public static void main(String[] args) throws Exception {
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        String log4jFile = System.getProperty("log4j.configurationFile");
        if (log4jFile == null || !(new File(log4jFile)).exists()) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            File log4j = new File("./etc/log4j2.xml");
            ctx.setConfigLocation(log4j.toURI());
        }

        ServerConfiguration configuration = new ServerConfiguration();
        configuration.setPort(9000);
        YamlUtils.objectToYamlFile(configuration, "etc/serverConf.yml");
        Server server = new Server(configuration, new Handler<IServerContext>() {
            @Override
            public void handle(IServerContext event) {
                InternalMessage request = event.getRequest();
                logger.info("receive request {}", request);
                event.sendResponse(request.getData());
            }
        });
        boolean start = server.start();
        if (start) {
            logger.error("Server started at {}", configuration.getPort());
        }
    }
}

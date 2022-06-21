package com.battle.client;

import com.battle.model.IClientHandler;
import com.battle.model.ISenderInfo;
import com.battle.model.InternalMessage;
import com.battle.utils.YamlUtils;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

public class SimpleClient {
    private static final Logger logger = LogManager.getLogger(SimpleClient.class);

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
                logger.info("Receive server response sender={} message={}", sender, message);
            }
        });
        String data = "{\"event\":\"walk\",\"data\":{\"gps\":[110000,110000],\"grav\":[12,212,122],\"step\":111}}";
        client.send("127.0.0.1", 9000, new InternalMessage(data));

    }
}

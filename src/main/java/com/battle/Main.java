package com.battle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

public class Main {
    private static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        String log4jFile = System.getProperty("log4j.configurationFile");
        if (log4jFile == null || !(new File(log4jFile)).exists()) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            File log4j = new File("./etc/log4j2.xml");
            ctx.setConfigLocation(log4j.toURI());
        }
        logger.info("Started");

    }
}
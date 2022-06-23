package com.battle.codec;

import com.battle.app.MongoConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MongoTest {
    @Test
    void testVertxMongo() throws ExecutionException, InterruptedException {
        MongoConfiguration conf = new MongoConfiguration();
        conf.setHost("localhost");
        conf.setPort(27017);
        conf.setDatabase("battle");
        JsonObject config = JsonObject.mapFrom(conf);
        Vertx vertx = Vertx.vertx();
        MongoClient client = MongoClient.createShared(vertx, config);
        JsonObject document = new JsonObject()
                .put("title", "The Hobbit");
        CompletableFuture<String> wait = new CompletableFuture<>();
        client.save("test", document, res -> {
            if (res.succeeded()) {
                String id = res.result();
                System.out.println("Saved book with id " + id);
                wait.complete(id);
            } else {
                res.cause().printStackTrace();
            }
        });
        String s = wait.get();
        System.out.println(s);
        JsonObject query = new JsonObject()
                .put("title", "The Hobbit");
        CompletableFuture<String> wait2 = new CompletableFuture<>();
        client.find("test", query, res -> {
            if (res.succeeded()) {
                for (JsonObject json : res.result()) {
                    System.out.println("query:" + json.encodePrettily());
                }
                wait2.complete("");
            } else {
                res.cause().printStackTrace();
            }
        });
        wait2.get();
    }
}

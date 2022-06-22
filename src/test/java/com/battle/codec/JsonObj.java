package com.battle.codec;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonObj {
    @Test
    void testJsonObject() {
        String data = "{\"event\":\"walk\",\"data\":{\"gps\":[110000,110000],\"grav\":[12,212,122],\"step\":111}}";
        JsonObject entries = new JsonObject(data);
        System.out.println(entries);
        assertEquals(true, entries.getMap().containsKey("data"));
    }
}

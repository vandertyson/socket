package com.battle.app;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class GsonService implements IJsonService {
    private final Gson gson = new Gson();

    @Override
    public String serialize(Object input) {
        return gson.toJson(input);
    }

    @Override
    public <T> T deserialize(Class<T> type, String input) {
        return gson.fromJson(input, type);
    }
}

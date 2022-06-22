package com.battle.app;

import com.jsoniter.Jsoniter;

public interface IJsonService {
    public abstract String serialize(Object input);

    public abstract <T> T deserialize(Class<T> type, String input);
}

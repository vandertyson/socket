package com.battle.model;

public class InternalMessage {
    private final String data;

    public InternalMessage(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }


    @Override
    public String toString() {
        return "InternalMessage{" +
                "size=" + data.length() +
                ", content=" + data +
                '}';
    }
}

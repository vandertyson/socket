package com.battle.model;

import java.util.Arrays;

public class InternalMessage {

    private byte[] data;
    private Long id;

    public InternalMessage() {

    }

    public InternalMessage(byte[] data, Long id) {
        this.data = data;
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "InternalMessage{" +
                "size=" + data.length +
                ", id=" + id +
                '}';
    }
}

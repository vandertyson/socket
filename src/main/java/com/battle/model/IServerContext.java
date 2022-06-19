package com.battle.model;

public interface IServerContext {
    public abstract InternalMessage getRequest();

    public abstract ISenderInfo getSenderInfo();

    public abstract void sendResponse(byte[] data);

    public abstract void sendResponse(String data);
}

package com.battle.model;

public interface IClientHandler {
    public abstract void onResponse(InternalMessage message, ISenderInfo sender);
}

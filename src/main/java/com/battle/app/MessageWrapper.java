package com.battle.app;

import com.battle.model.IServerContext;

public class MessageWrapper {
    private final IServerContext context;

    public MessageWrapper(IServerContext context) {
        this.context = context;
    }

    public IServerContext getContext() {
        return context;
    }
}

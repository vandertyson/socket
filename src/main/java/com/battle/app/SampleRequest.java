package com.battle.app;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SampleRequest {
    private static final AtomicLong requestIDGen = new AtomicLong();
    private final Long requestID;
    private final String userID;
    private final Integer type;
    private final Map<Long, Object> transactions = new HashMap<>();

    public SampleRequest(String userID, Integer type) {
        this.requestID = requestIDGen.incrementAndGet();
        this.userID = userID;
        this.type = type;
    }

    public String getUserID() {
        return userID;
    }

    public Integer getType() {
        return type;
    }

    public Map<Long, Object> getTransactions() {
        return transactions;
    }

    public Long getRequestID() {
        return requestID;
    }
}

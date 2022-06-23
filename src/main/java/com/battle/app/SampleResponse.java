package com.battle.app;

public class SampleResponse {
    public final Long requestID;
    public final String userID;

    public SampleResponse(Long requestId, String userId) {
        this.requestID = requestId;
        this.userID = userId;
    }

    public Long getRequestId() {
        return requestID;
    }

    public String getUserId() {
        return userID;
    }
}

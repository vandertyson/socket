package com.battle.app;

public class SampleResponse {
    public final Long requestID;
    public final String userID;
    public final Integer requestType;

    public SampleResponse(Long requestId, String userId, Integer requestType) {
        this.requestID = requestId;
        this.userID = userId;
        this.requestType = requestType;
    }

    public Integer getRequestType() {
        return requestType;
    }

    public Long getRequestId() {
        return requestID;
    }

    public String getUserId() {
        return userID;
    }
}

package com.battle.app;

public class ErrorMessage {
    public static class ErrorCode {
        public static final Integer FULL_QUEUE = 50;
        public static final Integer EXCEPTION = 51;
    }

    public final String message;
    public final Integer errorCode;

    public ErrorMessage(String message, Integer errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }
}

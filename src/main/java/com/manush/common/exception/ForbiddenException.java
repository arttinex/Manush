package com.manush.common.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
    public ForbiddenException() {
        super("Access denied");
    }
}

package org.routineimpulse.exception;

import java.time.Instant;

public class ApiErrorResponse {

    private int status;
    private String error;
    private String message;
    private Instant timestamp;
    private String path;

    public ApiErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        String path
    ) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}
package org.routineimpulse.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {

    private final Response.Status status;
    private final String error;

    public ApiException(String error, String message) {
        this(Response.Status.BAD_REQUEST, error, message);
    }

    public ApiException(Response.Status status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public ApiException(Response.Status status, String message) {
        this(status, null, message);
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
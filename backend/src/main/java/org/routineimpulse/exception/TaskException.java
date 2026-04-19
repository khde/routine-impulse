package org.routineimpulse.exception;

import jakarta.ws.rs.core.Response;

public class TaskException extends ApiException {

    public TaskException(String error, String message) {
        super(Response.Status.BAD_REQUEST, error, message);
    }

    public TaskException(Response.Status status, String error, String message) {
        super(status, error, message);
    }
}
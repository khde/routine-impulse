package org.routineimpulse.exception;

import jakarta.ws.rs.core.Response;

public class RoutineException extends ApiException {

    public RoutineException(String error, String message) {
        super(Response.Status.BAD_REQUEST, error, message);
    }

    public RoutineException(Response.Status status, String error, String message) {
        super(status, error, message);
    }
}
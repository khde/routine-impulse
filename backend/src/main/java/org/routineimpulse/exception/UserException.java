package org.routineimpulse.exception;

import jakarta.ws.rs.core.Response;

public class UserException extends ApiException {

    public UserException(String error, String message) {
        super(Response.Status.BAD_REQUEST, error, message);
    }

    public UserException(Response.Status status, String error, String message) {
        super(status, error, message);
    }
}
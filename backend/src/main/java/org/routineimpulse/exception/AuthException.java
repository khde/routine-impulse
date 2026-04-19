package org.routineimpulse.exception;

import jakarta.ws.rs.core.Response;

public class AuthException extends ApiException {

    public AuthException(String error, String message) {
        super(Response.Status.UNAUTHORIZED, error, message);
    }

    public AuthException(Response.Status status, String error, String message) {
        super(status, error, message);
    }
}
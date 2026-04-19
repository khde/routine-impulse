package org.routineimpulse.exception;

import java.time.Instant;

import io.quarkus.security.AuthenticationFailedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            Response.Status.UNAUTHORIZED.getStatusCode(),
            "INVALID_CREDENTIALS",
            "Invalid credentials",
            Instant.now(),
            uriInfo != null ? uriInfo.getPath() : null
        );

        return Response.status(Response.Status.UNAUTHORIZED)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
    }
}
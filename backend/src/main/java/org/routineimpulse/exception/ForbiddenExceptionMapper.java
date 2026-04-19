package org.routineimpulse.exception;

import java.time.Instant;

import io.quarkus.security.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ForbiddenException exception) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            Response.Status.FORBIDDEN.getStatusCode(),
            "ACCESS_FORBIDDEN",
            "Access forbidden",
            Instant.now(),
            uriInfo != null ? uriInfo.getPath() : null
        );

        return Response.status(Response.Status.FORBIDDEN)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
    }
}
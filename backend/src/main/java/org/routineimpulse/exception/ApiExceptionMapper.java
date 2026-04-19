package org.routineimpulse.exception;

import java.time.Instant;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ApiException exception) {
        Response.Status status = exception.getStatus();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            status.getStatusCode(),
            exception.getError(),
            exception.getMessage(),
            Instant.now(),
            uriInfo != null ? uriInfo.getPath() : null
        );

        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(errorResponse)
            .build();
    }
}
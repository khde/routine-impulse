package org.routineimpulse.resource;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
public class GatewayResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        return indexHtml();
    }

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.TEXT_HTML)
    public Response route(@PathParam("path") String path) {
        if (isApiOrFilePath(path)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return indexHtml();
    }

    private boolean isApiOrFilePath(String path) {
        return path.startsWith("api/")
            || path.equals("api")
            || path.startsWith("q/")
            || path.equals("q")
            || path.contains(".");
    }

    private Response indexHtml() {
        InputStream indexHtml = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("META-INF/resources/index.html");

        if (indexHtml == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(indexHtml)
            .type(MediaType.TEXT_HTML_TYPE)
            .build();
    }
}

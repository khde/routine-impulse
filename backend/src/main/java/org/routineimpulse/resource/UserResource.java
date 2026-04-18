package org.routineimpulse.resource;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.routineimpulse.service.UserService;

@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @Path("/profile")
    public Response getProfile() {
        return Response.ok(userService.getCurrentProfile()).build();
    }

    @GET
    @Path("/preferences")
    public Response getPreferences() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}

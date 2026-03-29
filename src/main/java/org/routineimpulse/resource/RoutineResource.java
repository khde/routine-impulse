package org.routineimpulse.resource;

import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.PathParam;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.service.RoutineService;

@Path("/api/routine")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoutineResource {

    @Inject
    RoutineService routineService;

    @Context
    SecurityContext securityContext;

    @POST
    public Response createRoutine(@Valid RoutineRequest request) {
        String currentUsername = securityContext.getUserPrincipal().getName();
        RoutineResponse response = routineService.createRoutine(request, currentUsername);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/all")
    public Response getAllRoutinesForUser() {
        String currentUsername = securityContext.getUserPrincipal().getName();
        List<RoutineResponse> routines = routineService.getAllRoutinesForUser(currentUsername);

        return Response.ok(routines).build();
    }

    @GET
    @Path("/{id}")
    public Response getRoutineById(@PathParam("id") Long id) {
        String currentUsername = securityContext.getUserPrincipal().getName();
        RoutineResponse response = routineService.getRoutineById(id, currentUsername);

        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoutine(@PathParam("id") Long id) {
        String currentUsername = securityContext.getUserPrincipal().getName();
        routineService.deleteRoutine(id, currentUsername);

        return Response.noContent().build();
    }
}

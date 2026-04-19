package org.routineimpulse.resource;

import java.util.List;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.PathParam;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Context;

import org.routineimpulse.dto.RoutineRequest;
import org.routineimpulse.dto.RoutineResponse;
import org.routineimpulse.dto.RoutineUpdateRequest;
import org.routineimpulse.service.RoutineService;
import org.routineimpulse.service.AuthService;

@Path("/routine")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoutineResource {

    @Inject
    RoutineService routineService;

    @Context
    AuthService authService;

    @POST
    public Response createRoutine(@Valid RoutineRequest request) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.createRoutine(request, username);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/all")
    public Response getAllRoutinesForUser() {
        String username = authService.getCurrentUsername();
        List<RoutineResponse> routines = routineService.getAllRoutinesForUser(username);

        return Response.ok(routines).build();
    }

    @GET
    @Path("/{id}")
    public Response getRoutineById(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.getRoutineById(id, username);

        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{id}")
    public Response updateRoutine(@PathParam("id") Long id, @Valid RoutineUpdateRequest request) {
        String username = authService.getCurrentUsername();
        RoutineResponse response = routineService.updateRoutine(id, request, username);

        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoutine(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        routineService.deleteRoutine(id, username);

        return Response.noContent().build();
    }
}

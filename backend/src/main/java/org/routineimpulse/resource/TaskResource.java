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

import org.routineimpulse.dto.TaskRequest;
import org.routineimpulse.dto.TaskResponse;
import org.routineimpulse.service.TaskService;
import org.routineimpulse.service.AuthService;

@Path("/api/task")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    @Inject
    TaskService taskService;

    @Context
    AuthService authService;

    @POST
    public Response createTask(@Valid TaskRequest request) {
        String username = authService.getCurrentUsername();
        TaskResponse response = taskService.createTask(request, username);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/all")
    public Response getAllTasksForUser() {
        String username = authService.getCurrentUsername();
        List<TaskResponse> tasks = taskService.getAllTasksForUser(username);

        return Response.ok(tasks).build();
    }

    @GET
    @Path("/{id}")
    public Response getTaskById(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        TaskResponse response = taskService.getTaskById(id, username);

        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTask(@PathParam("id") Long id) {
        String username = authService.getCurrentUsername();
        taskService.deleteTask(id, username);

        return Response.noContent().build();
    }
}

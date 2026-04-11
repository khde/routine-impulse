package org.routineimpulse.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.persistence.NoResultException;

import org.routineimpulse.dto.TaskRequest;
import org.routineimpulse.dto.TaskResponse;
import org.routineimpulse.model.Task;
import org.routineimpulse.model.User;

@ApplicationScoped
public class TaskService {

    @Inject
    EntityManager em;

    @Transactional
    public TaskResponse createTask(TaskRequest request, String username) {
        Task task = new Task();
        task.setDescription(request.getDescription());
        task.setCompleted(request.isCompleted());
        task.setDueDate(request.getDueDate());

        task.setUser(findUserByUsername(username));

        em.persist(task);

        return mapToResponse(task);
    }

    public List<TaskResponse> getAllTasksForUser(String username) {
        List<Task> tasks = em.createQuery(
            "SELECT t FROM Task t WHERE t.user.username = :username", Task.class)
            .setParameter("username", username)
            .getResultList();

        return tasks.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id, String username) {
        try {
            Task task = em.createQuery(
                "SELECT t FROM Task t WHERE t.id = :id AND t.user.username = :username", Task.class)
                .setParameter("id", id)
                .setParameter("username", username)
                .getSingleResult();

            return mapToResponse(task);
        } catch (NoResultException e) {
            throw new NotFoundException("Task not found");
        }
    }

    @Transactional
    public void deleteTask(Long id, String username) {
        try {
            Task task = em.createQuery(
                "SELECT t FROM Task t WHERE t.id = :id AND t.user.username = :username", Task.class)
                .setParameter("id", id)
                .setParameter("username", username)
                .getSingleResult();

            em.remove(task);
        } catch (NoResultException e) {
            throw new NotFoundException("Task not found");
        }
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setDescription(task.getDescription());
        response.setCompleted(task.isCompleted());
        response.setDueDate(task.getDueDate());
        response.setCreationDate(task.getCreationDate());

        return response;
    }

    private User findUserByUsername(String username) {
        return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
            .setParameter("username", username)
            .getSingleResult();
    }
}


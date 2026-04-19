package org.routineimpulse.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class TaskUpdateRequest {

    @Pattern(regexp = "^$|.*\\S.*", message = "Description must not be blank")
    @Size(max = 255, message = "Description is too long")
    private String description;

    private Boolean completed;

    @FutureOrPresent(message = "Due date must not be in the past")
    private LocalDateTime dueDate;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}

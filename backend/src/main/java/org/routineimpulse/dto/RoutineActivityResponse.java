package org.routineimpulse.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RoutineActivityResponse {

    private LocalDate date;
    private boolean completed;
    private LocalDateTime updatedDate;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }
}
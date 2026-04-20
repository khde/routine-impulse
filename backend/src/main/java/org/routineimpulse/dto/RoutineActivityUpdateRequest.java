package org.routineimpulse.dto;

import jakarta.validation.constraints.NotBlank;

public class RoutineActivityUpdateRequest {

    @NotBlank(message = "Date is required")
    private String date;

    private boolean completed;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
package org.routineimpulse.dto;

import java.time.DayOfWeek;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;

public class RoutineRequest {
    @NotBlank(message = "Routine name must not be blank")
    private String name;

    private String description;

    private Set<DayOfWeek> selectedDays;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DayOfWeek> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(Set<DayOfWeek> selectedDays) {
        this.selectedDays = selectedDays;
    }
}

package org.routineimpulse.dto;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.Set;

public class RoutineResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime creationDate;
    private Set<DayOfWeek> selectedDays;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Set<DayOfWeek> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(Set<DayOfWeek> selectedDays) {
        this.selectedDays = selectedDays;
    }
}

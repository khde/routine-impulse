package org.routineimpulse.model;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.HashSet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "routine_schedules")
public class RoutineSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "schedule")
    private Routine routine;


    @ElementCollection
    @CollectionTable(name = "routine_weekdays", 
                     joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> selectedDays = new HashSet<>();

    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }

    public void addDay(DayOfWeek day) {
        if (day != null) {
            this.selectedDays.add(day);
        }
    }

    public void removeDay(DayOfWeek day) {
        this.selectedDays.remove(day);
    }

    public void clearDays() {
        this.selectedDays.clear();
    }

    public Long getId() {
        return id;
    }

    public Routine getRoutine() {
        return routine;
    }

    public void setRoutine(Routine routine) {
        this.routine = routine;
    }

    public Set<DayOfWeek> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(Set<DayOfWeek> selectedDays) {
        if (selectedDays != null) {
            this.selectedDays = selectedDays;
        } else {
            this.selectedDays = new HashSet<>();
        }
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}

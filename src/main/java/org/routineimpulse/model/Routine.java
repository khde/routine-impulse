package org.routineimpulse.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "routines")
public class Routine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String recurrence;
    
    @Column(updatable = false)
    private String description;

    @Column(name = "create_date", updatable = false)
    @JsonFormat(pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime createDate;

    @Column(name = "modify_date")
    @JsonFormat(pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime modifyDate;

    public Routine() {
    }

    public Routine(String name, String description, String recurrence) {
        this.name = name;
        this.description = description;
        this.recurrence = recurrence;
        this.createDate = LocalDateTime.now();
        this.modifyDate = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createDate = LocalDateTime.now();
        modifyDate= LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifyDate = LocalDateTime.now();
    }

    public long getId() {
        return id;
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

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime date) {
        createDate = date;
    }

    public LocalDateTime getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(LocalDateTime date) {
        modifyDate = date;
    }
}

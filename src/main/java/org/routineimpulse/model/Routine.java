package org.routineimpulse.model;

import java.time.LocalDateTime;

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
    
    @Column(updatable = false)
    private String description;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "modify_date")
    private LocalDateTime modifyDate;

    public Routine() {
    }

    public Routine(String name, String description) {
        this.name = name;
        this.description = description;
        this.createDate = LocalDateTime.now();
        this.modifyDate= LocalDateTime.now();
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

    public void setDescripton(String description) {
        this.description = description;
    }

    public LocalDateTime getCreationDate() {
        return createDate;
    }

    public void setCreationDate(LocalDateTime date) {
        createDate = date;
    }

    public LocalDateTime getUpdateDate() {
        return modifyDate;
    }

    public void setUpdateDate(LocalDateTime date) {
        modifyDate = date;
    }
}

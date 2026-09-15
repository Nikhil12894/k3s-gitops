package com.explorewithnk.appcore.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;

@Entity
@Table(name = "tasks")
@RegisterForReflection
public class TaskItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String title;

    @Column(length = 500)
    public String description;

    @Column(nullable = false, length = 30)
    public String status = "PENDING"; // PENDING, IN_PROGRESS, COMPLETED

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    public TaskItem() {
    }

    public TaskItem(String title, String description) {
        this.title = title;
        this.description = description;
        this.status = "PENDING";
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onPreUpdate() {
        updatedAt = Instant.now();
    }
}

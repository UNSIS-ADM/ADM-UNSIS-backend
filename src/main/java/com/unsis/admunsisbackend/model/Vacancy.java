package com.unsis.admunsisbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies", uniqueConstraints = @UniqueConstraint(name = "ux_career_year", columnNames = { "career",
        "admission_year" }))
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String career;

    @Column(name = "admission_year", nullable = false)
    private Integer admissionYear;

    @Column(name = "limit_count", nullable = false)
    private Integer limitCount;

    @Column(name = "accepted_count", nullable = false)
    private Integer acceptedCount;

    @Column(name = "pending_count", nullable = false)
    private Integer pendingCount;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // getters & setters...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
    }

    public Integer getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(Integer acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getLimitCount() {
        return limitCount;
    }

    public void setLimitCount(Integer limitCount) {
        this.limitCount = limitCount;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}

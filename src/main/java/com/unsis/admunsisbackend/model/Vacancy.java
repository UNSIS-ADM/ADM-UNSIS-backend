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

    @Column(name = "inscritos_count", nullable = false)
    private Integer inscritosCount = 0;

    @Column(name = "cupos_inserted", nullable = false)
    private Integer cuposInserted = 0;

    @Column(name = "reserved_count", nullable = false)
    private Integer reservedCount = 0;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "limit_count")
    private Integer limitCount = 0;

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
    public Integer getLimitCount() {
        return limitCount;
    }

    public void setLimitCount(Integer limitCount) {
        this.limitCount = limitCount;
    }

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

    public Integer getInscritosCount() {
        return inscritosCount;
    }

    public void setInscritosCount(Integer inscritosCount) {
        this.inscritosCount = inscritosCount;
    }

    public Integer getCuposInserted() {
        return cuposInserted;
    }

    public void setCuposInserted(Integer cuposInserted) {
        this.cuposInserted = cuposInserted;
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

    public Integer getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(Integer reservedCount) {
        this.reservedCount = reservedCount;
    }

}

// src/main/java/com/unsis/admunsisbackend/dto/ApplicantAdminUpdateDTO.java
package com.unsis.admunsisbackend.dto;

import com.unsis.admunsisbackend.model.ApplicantStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApplicantAdminUpdateDTO {
    // opcional: puedes incluir id pero lo validamos en controller si viene
    private Long id;

    @Min(1)
    private Long ficha;

    // CURP: 18 caracteres alfa-numéricos (mayúsculas). Ajusta regex si necesitas
    // reglas más estrictas.
    @Pattern(regexp = "^[A-Z0-9]{18}$", message = "CURP inválida (18 caracteres, mayúsculas y números)")
    private String curp;

    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String career;

    @Size(max = 100)
    private String location;

    @Size(max = 100)
    private String examRoom;

    private Boolean examAssigned;

    // ISO-8601 expected e.g. "2026-06-01T09:00:00"
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime examDate;

    // Usaremos enum ApplicantStatus (null = no cambiar)
    private ApplicantStatus status;

    @Min(2000)
    private Integer admissionYear;

    // lastLogin en User (ISO-8601)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;

    private String careerAtResult;
    private BigDecimal score;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFicha() {
        return ficha;
    }

    public void setFicha(Long ficha) {
        this.ficha = ficha;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCareer() {
        return career;
    }

    public void setCareer(String career) {
        this.career = career;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExamRoom() {
        return examRoom;
    }

    public void setExamRoom(String examRoom) {
        this.examRoom = examRoom;
    }

    public Boolean getExamAssigned() {
        return examAssigned;
    }

    public void setExamAssigned(Boolean examAssigned) {
        this.examAssigned = examAssigned;
    }

    public LocalDateTime getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDateTime examDate) {
        this.examDate = examDate;
    }

    public ApplicantStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicantStatus status) {
        this.status = status;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getCareerAtResult() {
        return careerAtResult;
    }

    public void setCareerAtResult(String careerAtResult) {
        this.careerAtResult = careerAtResult;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

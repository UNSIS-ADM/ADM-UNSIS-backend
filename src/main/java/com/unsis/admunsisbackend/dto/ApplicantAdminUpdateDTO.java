// src/main/java/com/unsis/admunsisbackend/dto/ApplicantAdminUpdateDTO.java
package com.unsis.admunsisbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApplicantAdminUpdateDTO {
    
    @Min(1)
    private Long ficha;

    @Pattern(regexp = "^[A-Z0-9]{18}$", message = "CURP inválida (18 caracteres, mayúsculas y números)")
    private String curp;

    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String career;

    @Size(max = 100)
    private String location;

    private BigDecimal finalGrade;

    @Size(max = 100)
    private String examRoom;

    private Boolean examAssigned;

    // ISO-8601 expected e.g. "2026-06-01T09:00:00"
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime examDate; //Fecha de examen para el aspirante

    @Min(2000)
    private Integer admissionYear;

    private String careerAtResult;

    @DecimalMin(value = "0", inclusive = true, message = "El score no puede ser menor que 0")
    @DecimalMax(value = "100", inclusive = true, message = "El score no puede ser mayor que 100")
    private BigDecimal score;

    // getters y setters
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

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
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

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

}

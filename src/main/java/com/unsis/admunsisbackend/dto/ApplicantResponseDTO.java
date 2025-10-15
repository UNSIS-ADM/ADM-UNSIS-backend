package com.unsis.admunsisbackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ApplicantResponseDTO {

    private Long id;
    private Long ficha;
    private String curp;
    private String careerAtResult; 
    private String fullName;
    private String career;
    private String location;
    private String examRoom;
    private LocalDateTime examDate;
    private String status;
    private String comment;
    private BigDecimal score;
    private BigDecimal finalGrade;
    private String AttendanceStatus; // ASISTIO | ASISTIÓ | NP
    private Integer admissionYear;
    private LocalDateTime lastLogin;
    private LocalDateTime resultDate;

    public LocalDateTime getResultDate() {
        return resultDate;
    }

    public void setResultDate(LocalDateTime resultDate) {
        this.resultDate = resultDate;
    }

    // Getters y Setters
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

    public String getCareerAtResult() {
        return careerAtResult;
    }

    public void setCareerAtResult(String careerAtResult) {
        this.careerAtResult = careerAtResult;
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

    public LocalDateTime getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDateTime examDate) {
        this.examDate = examDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public String getAttendanceStatus() {
        return AttendanceStatus;
    }

    public void setAttendanceStatus(String AttendanceStatus) {
        this.AttendanceStatus = AttendanceStatus;
    }

}

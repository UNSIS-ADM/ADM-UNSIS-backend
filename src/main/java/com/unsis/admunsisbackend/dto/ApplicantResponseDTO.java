package com.unsis.admunsisbackend.dto;

import java.time.LocalDateTime;

public class ApplicantResponseDTO {

    
    private Long id;
    private String curp;
    private String fullName;
    private String career;
    private String location;
    private String phone;
    private String examRoom;
    private LocalDateTime examDate;
    private String status;
    private LocalDateTime lastLogin;

    // Getters y Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
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
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
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
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}

package com.unsis.admunsisbackend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;

@Entity
@Table(name = "applicants", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "ficha", "admission_year" }),
        @UniqueConstraint(columnNames = { "curp", "admission_year" })
})
public class Applicant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK al usuario interno
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private Long ficha; 

    @Column(nullable = false)
    private String curp;

    private String career;
    private String location;

    @Column(name = "career_at_result")
    private String careerAtResult;

    @Column(name = "exam_assigned")
    private Boolean examAssigned = false;

    @Column(name = "exam_room")
    private String examRoom;

    @Column(name = "exam_date")
    private LocalDateTime examDate;

    private String status = "PENDING";

    private BigDecimal finalGrade;

    @Column(name = "attendance_status", length = 10)
    private String AttendanceStatus; // "ASISTIO" | "NP"

    @Column(name = "admission_year", nullable = false)
    private Integer admissionYear;

    public Applicant() {
        this.admissionYear = Year.now().getValue();
    }

    public Integer getAdmissionYear() {
        return this.admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = (admissionYear != null) ? admissionYear : Year.now().getValue();
    }

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

    public Boolean getExamAssigned() {
        return examAssigned;
    }

    public void setExamAssigned(Boolean examAssigned) {
        this.examAssigned = examAssigned;
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

    public BigDecimal getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(BigDecimal finalGrade) {
        this.finalGrade = finalGrade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAttendanceStatus() {
        return AttendanceStatus;
    }

    public void setAttendanceStatus(String AttendanceStatus) {
        this.AttendanceStatus = AttendanceStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCareerAtResult() {
        return careerAtResult;
    }

    public void setCareerAtResult(String careerAtResult) {
        this.careerAtResult = careerAtResult;
    }

}

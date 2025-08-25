package com.unsis.admunsisbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.Year;


@Entity
@Table(name = "applicants")
public class Applicant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< Updated upstream
    // FK al usuario interno
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false, unique = true)
    private Long ficha; // <-- Nuevo

    @Column(nullable = false, unique = true)
=======
    @Column(nullable = false)
    private Long ficha;

    @Column(name = "curp", unique = true, nullable = false)
>>>>>>> Stashed changes
    private String curp;
    private String career;
    private String location;
    private String phone;

    @Column(name = "exam_assigned")
    private Boolean examAssigned = false;

    @Column(name = "exam_room")
    private String examRoom;

    @Column(name = "exam_date")
    private LocalDateTime examDate;

    private String status = "PENDING";

    @Column(name="admission_year", nullable=false)
    private Integer admissionYear;
    
    public Applicant() {
        this.admissionYear = Year.now().getValue(); // Asignación automática del año actual
    }

    public Integer getAdmissionYear() {
        return this.admissionYear;
    }
    // Getters y Setters
    // Setter con lógica para asignar año actual si es null
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
<<<<<<< Updated upstream
    
=======

>>>>>>> Stashed changes
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}

package com.admunisis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "aspirantes")
public class Aspirante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(unique = true, nullable = false)
    private String curp;

    @Column(nullable = false)
    private String carreraSolicitada;

    @Column(nullable = false)
    private String estatus = "PENDIENTE"; // PENDIENTE, ACEPTADO, RECHAZADO, EN_ESPERA

    private LocalDateTime fechaExamen;
    private String aulaExamen;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }
    public String getCarreraSolicitada() { return carreraSolicitada; }
    public void setCarreraSolicitada(String carreraSolicitada) { this.carreraSolicitada = carreraSolicitada; }
    public String getEstatus() { return estatus; }
    public void setEstatus(String estatus) { this.estatus = estatus; }
    public LocalDateTime getFechaExamen() { return fechaExamen; }
    public void setFechaExamen(LocalDateTime fechaExamen) { this.fechaExamen = fechaExamen; }
    public String getAulaExamen() { return aulaExamen; }
    public void setAulaExamen(String aulaExamen) { this.aulaExamen = aulaExamen; }
}

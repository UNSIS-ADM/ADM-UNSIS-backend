package com.admunisis.model;

import javax.persistence.*;

@Entity
@Table(name = "carreras")
public class Carrera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private Integer cupoMaximo;

    @Column(nullable = false)
    private Integer cupoDisponible;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(Integer cupoMaximo) { this.cupoMaximo = cupoMaximo; }
    public Integer getCupoDisponible() { return cupoDisponible; }
    public void setCupoDisponible(Integer cupoDisponible) { this.cupoDisponible = cupoDisponible; }
}

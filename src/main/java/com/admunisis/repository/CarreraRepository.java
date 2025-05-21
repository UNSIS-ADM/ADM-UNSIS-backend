package com.admunisis.repository;

import com.admunisis.model.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
    Optional<Carrera> findByNombre(String nombre);
    List<Carrera> findByCupoDisponibleGreaterThan(Integer cupo);
}

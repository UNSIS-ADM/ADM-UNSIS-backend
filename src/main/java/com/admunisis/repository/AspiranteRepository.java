package com.admunisis.repository;

import com.admunisis.model.Aspirante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AspiranteRepository extends JpaRepository<Aspirante, Long> {
    List<Aspirante> findByCarreraSolicitada(String carrera);
    List<Aspirante> findByEstatus(String estatus);
    boolean existsByCurp(String curp);
}

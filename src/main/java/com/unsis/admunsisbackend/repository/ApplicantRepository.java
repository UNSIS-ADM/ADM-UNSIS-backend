package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    //Optional<Applicant> findByUser_Id(Long userId);    
    Optional<Applicant> findByFicha(Long ficha);
    boolean existsByFicha(Long ficha);
    
    Optional<Applicant> findByCurp(String curp);
    boolean existsByCurp(String curp);

    // Método para encontrar un Applicant por el nombre de usuario del User asociado
    Optional<Applicant> findByUser_Username(String username);

}




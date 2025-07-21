package com.unsis.admunsisbackend.repository;
    
import com.unsis.admunsisbackend.model.AdmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionResultRepository extends JpaRepository<AdmissionResult, Long> {
    //boolean existsByFicha(Long ficha);
    // opcional:
    //boolean existsByApplicantId(Long applicantId);
}

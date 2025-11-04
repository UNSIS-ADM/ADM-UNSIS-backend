package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.AdmissionResult;
import com.unsis.admunsisbackend.model.Applicant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/* Repositorio para los resultados de admisión */
public interface AdmissionResultRepository extends JpaRepository<AdmissionResult, Long> {
    // Trae el más reciente (ORDER BY createdAt DESC LIMIT 1)
    Optional<AdmissionResult> findTopByApplicantOrderByCreatedAtDesc(Applicant applicant);

}

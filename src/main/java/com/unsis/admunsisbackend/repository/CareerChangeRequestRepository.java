package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.CareerChangeRequest;
import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

/* Repositorio para las solicitudes de cambio de carrera */
public interface CareerChangeRequestRepository
    extends JpaRepository<CareerChangeRequest, Long> {
      
  // Búsquedas comunes
  Optional<CareerChangeRequest> findByApplicantAndStatus(Applicant app, String status);
  List<CareerChangeRequest> findByStatus(String status);
  List<CareerChangeRequest> findByApplicant(Applicant applicant);

  // Cuenta cuántas solicitudes pendientes hay para una carrera y año específico
  @Query("""
      SELECT COUNT(r) FROM CareerChangeRequest r JOIN r.applicant a
      WHERE UPPER(r.newCareer) = UPPER(:career)
        AND a.admissionYear = :year
        AND UPPER(r.status) = 'PENDIENTE'
      """)
  long countPendingByNewCareerAndYear(@Param("career") String career, @Param("year") int year);
}

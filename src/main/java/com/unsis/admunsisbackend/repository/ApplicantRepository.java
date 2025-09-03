package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
   Optional<Applicant> findByFicha(Long ficha);
   boolean existsByFicha(Long ficha);
   Optional<Applicant> findByCurp(String curp);
   boolean existsByCurp(String curp);

   // Posibles busquedas
   List<Applicant> findByCurpContainingIgnoreCase(String curp);
   List<Applicant> findByCareerContainingIgnoreCase(String career);
   List<Applicant> findByUser_FullNameContainingIgnoreCase(String fullName);
   Optional<Applicant> findByUser_Username(String username);
   List<Applicant> findByAdmissionYear(Integer admissionYear);

   long countByCareerAndAdmissionYear(String career, int admissionYear);
   long countByCareerAndAdmissionYearAndStatus(String career, int year, String status);

}

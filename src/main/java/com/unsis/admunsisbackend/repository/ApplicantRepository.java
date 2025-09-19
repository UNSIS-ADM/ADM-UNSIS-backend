package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

   boolean existsByFichaAndAdmissionYear(Long ficha, Integer admissionYear);

   boolean existsByCurpAndAdmissionYear(String curp, Integer admissionYear);

   @Query("SELECT a.career as career, COUNT(a) as cnt FROM Applicant a WHERE a.admissionYear = :year GROUP BY a.career")
   List<Object[]> countApplicantsGroupedByCareer(@Param("year") int year);

}

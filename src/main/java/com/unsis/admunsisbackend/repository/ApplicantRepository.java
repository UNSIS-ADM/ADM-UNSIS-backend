package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

/* Repositorio para los aspirantes */
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
   // Búsquedas directas
   Optional<Applicant> findByFicha(String ficha);
   boolean existsByFicha(String ficha);
   Optional<Applicant> findByCurp(String curp);
   boolean existsByCurp(String curp);

   // Posibles busquedas
   List<Applicant> findByCurpContainingIgnoreCase(String curp);
   List<Applicant> findByCareerContainingIgnoreCase(String career);
   List<Applicant> findByUser_FullNameContainingIgnoreCase(String fullName);
   Optional<Applicant> findByUser_Username(String username);
   List<Applicant> findByAdmissionYear(Integer admissionYear);
   
   Long countByCareerAndAdmissionYear(String career, int admissionYear);
   boolean existsByFichaAndAdmissionYear(String ficha, Integer admissionYear);
   boolean existsByCurpAndAdmissionYear(String curp, Integer admissionYear);
   
   Page<Applicant> findByAdmissionYear(Integer year, Pageable pageable);
   
   @Query("""     
         SELECT a FROM Applicant a
         WHERE
         (:year IS NULL OR a.admissionYear = :year)
         AND
         (:career IS NULL OR LOWER(a.career) LIKE LOWER(CONCAT('%', :career, '%')))
         AND
         (:status IS NULL OR LOWER(a.status) LIKE LOWER(CONCAT('%', :status, '%')))
         AND
         (
             :search IS NULL
             OR LOWER(a.curp) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR CAST(a.ficha AS string) LIKE CONCAT('%', :search, '%')
         )
         """)
   Page<Applicant> searchApplicants(
         @Param("year") Integer year,
         @Param("career") String career,
         @Param("status") String status,
         @Param("search") String search,
         Pageable pageable);
   
   // Conteo agrupado por carrera para un año de admisión dado
   @Query("SELECT a.career as career, COUNT(a) as cnt FROM Applicant a WHERE a.admissionYear = :year GROUP BY a.career")
   List<Object[]> countApplicantsGroupedByCareer(@Param("year") int year);

   @Query("SELECT DISTINCT a.career FROM Applicant a ORDER BY a.career ASC")
   List<String> findDistinctCareers();

}


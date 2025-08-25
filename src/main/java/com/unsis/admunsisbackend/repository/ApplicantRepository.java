package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;


public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    // Optional<Applicant> findByUser_Id(Long userId);
    Optional<Applicant> findByFicha(Long ficha);
    boolean existsByFicha(Long ficha);
    
    Optional<Applicant> findByCurp(String curp);
    boolean existsByCurp(String curp);

    List<Applicant> findByCurpContainingIgnoreCase(String curp);
    List<Applicant> findByCareerContainingIgnoreCase(String career);
    List<Applicant> findByUser_FullNameContainingIgnoreCase(String fullName);
    Optional<Applicant> findByUser_Username(String username);


    
    long countByCareerAndAdmissionYear(String career, int admissionYear);
    long countByCareerAndAdmissionYearAndStatus(String career, int year, String status);
    
    
 /**
  * 💡 Importante: las búsquedas de Vacancy se deben hacer en VacancyRepository,
  * no en ApplicantRepository.
  */
   // Optional<Vacancy> findByCareerAndAdmissionYear(String career, int admissionYear);
    


}



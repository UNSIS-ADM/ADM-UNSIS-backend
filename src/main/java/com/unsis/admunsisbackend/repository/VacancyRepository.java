package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    Optional<Vacancy> findByCareerAndAdmissionYear(String career, int admissionYear);
    Optional<Vacancy> findByCareerAndAdmissionYear(String career, Integer year);
    List<Vacancy> findByAdmissionYear(Integer year);
}

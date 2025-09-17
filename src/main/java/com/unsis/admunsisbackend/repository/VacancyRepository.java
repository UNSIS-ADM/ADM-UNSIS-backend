package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Vacancy;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vacancy> findByCareerAndAdmissionYear(String career, int admissionYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vacancy v where v.career = :career and v.admissionYear = :year")
    Optional<Vacancy> findByCareerAndAdmissionYearForUpdate(String career, int year);

    List<Vacancy> findByAdmissionYear(int admissionYear);

}

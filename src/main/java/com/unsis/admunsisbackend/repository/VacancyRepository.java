package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Vacancy;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vacancy> findByCareerAndAdmissionYear(String career, int admissionYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vacancy v where v.career = :career and v.admissionYear = :year")
    Optional<Vacancy> findByCareerAndAdmissionYearForUpdate(String career, int year);

    List<Vacancy> findByAdmissionYear(int admissionYear);

    @Modifying
    @Transactional
    @Query("UPDATE Vacancy v SET v.cuposInserted = :cupos, v.availableSlots = :available WHERE v.career = :career AND v.admissionYear = :year")
    int updateCuposAndAvailableSlots(@Param("career") String career,
            @Param("year") int year,
            @Param("cupos") int cuposInserted,
            @Param("available") int availableSlots);

}

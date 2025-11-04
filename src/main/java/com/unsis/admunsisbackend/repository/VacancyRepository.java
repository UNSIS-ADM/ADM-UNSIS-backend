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

/* Repositorio para las vacantes */
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    // Buscar por career y admissionYear con bloqueo de escritura
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vacancy> findByCareerAndAdmissionYear(String career, int admissionYear);

    // Buscar por career y admissionYear con bloqueo de escritura (método alternativo)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vacancy v where v.career = :career and v.admissionYear = :year")
    Optional<Vacancy> findByCareerAndAdmissionYearForUpdate(String career, int year);

    // Buscar todas las vacantes para un año de admisión específico
    List<Vacancy> findByAdmissionYear(int admissionYear);

    // Actualizar cuposInserted y availableSlots para una carrera y año específicos
    @Modifying
    @Transactional
    @Query("UPDATE Vacancy v SET v.cuposInserted = :cupos, v.availableSlots = :available WHERE v.career = :career AND v.admissionYear = :year")
    int updateCuposAndAvailableSlots(@Param("career") String career,
            @Param("year") int year,
            @Param("cupos") int cuposInserted,
            @Param("available") int availableSlots);

    /*
     * Buscar por career y admissionYear (sin bloqueo), normaliza (TRIM + UPPER)
     * para evitar falsos mismatches por espacios/case
     */
    @Query("SELECT v FROM Vacancy v WHERE UPPER(TRIM(v.career)) = UPPER(TRIM(:career)) AND v.admissionYear = :year")
    Optional<Vacancy> findByCareerAndAdmissionYear(@Param("career") String career, @Param("year") Integer year);

}

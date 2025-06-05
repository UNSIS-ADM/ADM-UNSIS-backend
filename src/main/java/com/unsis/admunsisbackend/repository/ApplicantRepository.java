package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByUser_Id(Long userId);
}

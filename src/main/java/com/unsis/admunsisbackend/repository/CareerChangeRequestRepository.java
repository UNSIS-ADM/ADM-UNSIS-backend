package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.CareerChangeRequest;
import com.unsis.admunsisbackend.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CareerChangeRequestRepository
    extends JpaRepository<CareerChangeRequest, Long> {

  Optional<CareerChangeRequest> findByApplicantAndStatus(Applicant app, String status);
  List<CareerChangeRequest> findByStatus(String status);
  List<CareerChangeRequest> findByApplicant(Applicant applicant);

}


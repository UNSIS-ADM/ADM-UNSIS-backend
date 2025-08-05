package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import java.util.List;

public interface ApplicantService {
    List<ApplicantResponseDTO> getAllApplicants();

    void changeCareerByCurp(String curp, String newCareer) throws RuntimeException;

    List<ApplicantResponseDTO> searchApplicants(
        Long ficha,
        String curp,
        String career,
        String fullName
    );
}

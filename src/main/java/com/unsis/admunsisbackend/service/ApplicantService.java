package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import java.util.List;

public interface ApplicantService {
    List<ApplicantResponseDTO> getAllApplicants();
}

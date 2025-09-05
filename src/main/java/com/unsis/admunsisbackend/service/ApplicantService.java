package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantAdminUpdateDTO;

import java.util.List;

public interface ApplicantService {
    List<ApplicantResponseDTO> getAllApplicants(Integer admissionYear);

    void changeCareerByCurp(String curp, String newCareer);

    List<ApplicantResponseDTO> searchApplicants(
            Long ficha,
            String curp,
            String career,
            String fullName);

    // ---------- Nuevos métodos para admin ----------
    ApplicantResponseDTO getApplicantById(Long id); // GET /api/admin/applicants/{id}
    ApplicantResponseDTO updateApplicantByAdmin(Long id, ApplicantAdminUpdateDTO dto, String adminUsername);

}

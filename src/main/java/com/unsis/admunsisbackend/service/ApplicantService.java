package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantAdminUpdateDTO;
import java.util.List;

// Servicio para gestionar los postulantes.
public interface ApplicantService {
    // Obtiene todos los postulantes para un año de admisión específico.
    List<ApplicantResponseDTO> getAllApplicants(Integer admissionYear);

    // Cambia la carrera de un postulante basado en su CURP.
    void changeCareerByCurp(String curp, String newCareer);
    List<ApplicantResponseDTO> searchApplicants(
            Long ficha,
            String curp,
            String career,
            String fullName);

    // Métodos para admin, obtiene un postulante por su ID.
    ApplicantResponseDTO getApplicantById(Long id);

    // Actualiza un postulante por un administrador.
    ApplicantResponseDTO updateApplicantByAdmin(
        Long id,
        ApplicantAdminUpdateDTO dto,
        String adminUsername);

}

package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantAdminUpdateDTO;
import org.springframework.data.domain.Page;
import java.util.List;

// Servicio para gestionar los postulantes.
public interface ApplicantService {
    // Obtiene todos los postulantes para un año de admisión específico.
    // List<ApplicantResponseDTO> getAllApplicants(Integer admissionYear);

    // Obtener aspirantes paginados
    Page<ApplicantResponseDTO> getAllApplicants(
            Integer admissionYear,
            int page,
            int size);

    // Búsqueda global con filtros y paginación
    Page<ApplicantResponseDTO> searchApplicants(
            Integer year,
            String career,
            String status,
            String search,
            int page,
            int size);

    // Cambia la carrera de un postulante basado en su CURP.
    void changeCareerByCurp(
            String curp,
            String newCareer);

    // Métodos para admin, obtiene un postulante por su ID.
    ApplicantResponseDTO getApplicantById(Long id);

    // Actualiza un postulante por un administrador.
    ApplicantResponseDTO updateApplicantByAdmin(
            Long id,
            ApplicantAdminUpdateDTO dto,
            String adminUsername);

    List<String> getAllCareers();

}

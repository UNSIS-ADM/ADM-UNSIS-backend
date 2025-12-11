package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantProfileDTO;

// Servicio para gestionar los perfiles de los postulantes.
public interface ApplicantProfileService {
    // Obtiene el perfil del postulante basado en su nombre de usuario.
    ApplicantProfileDTO getMyProfile(String username);
}

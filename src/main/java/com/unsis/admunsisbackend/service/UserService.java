package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import java.util.List;
import java.util.Set;
import com.unsis.admunsisbackend.dto.AdminUserUpdateDTO;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    ApplicantResponseDTO getApplicantProfile(String username);
    // Crea/actualiza el User asociado al Applicant: username = ficha, password = bcrypt(curp)
    void syncUserCredentialsForApplicant(Applicant applicant);

    UserResponseDTO adminCreateOrUpdateUser(AdminUserUpdateDTO dto);

}
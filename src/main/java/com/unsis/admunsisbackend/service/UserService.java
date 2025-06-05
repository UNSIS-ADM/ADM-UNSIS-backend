package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import java.util.List;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    ApplicantResponseDTO getApplicantProfile(String username);
}
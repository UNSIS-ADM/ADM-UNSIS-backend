package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ApplicantProfileDTO;

public interface ApplicantProfileService {
    ApplicantProfileDTO getMyProfile(String username);
}

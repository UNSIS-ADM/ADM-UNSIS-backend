package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ApplicantProfileDTO;
import com.unsis.admunsisbackend.service.ApplicantProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applicant")
public class ApplicantProfileController {

    @Autowired private ApplicantProfileService service;

    @GetMapping("/me")
    public ResponseEntity<ApplicantProfileDTO> getMyProfile(Authentication auth) {
        // Authentication#getName() devuelve el username (que es la ficha)
        String username = auth.getName();
        ApplicantProfileDTO profile = service.getMyProfile(username);
        return ResponseEntity.ok(profile);
    }
}

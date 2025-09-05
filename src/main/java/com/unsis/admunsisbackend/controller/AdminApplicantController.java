// src/main/java/com/unsis/admunsisbackend/controller/AdminApplicantController.java
package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ApplicantAdminUpdateDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.service.ApplicantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/admin/applicants")
public class AdminApplicantController {

    @Autowired
    private ApplicantService applicantService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApplicantResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicantService.getApplicantById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApplicantResponseDTO> updateByAdmin(
            @PathVariable Long id,
            @RequestBody @Valid ApplicantAdminUpdateDTO dto,
            Authentication auth) {
        String adminUsername = auth != null ? auth.getName() : "unknown";
        ApplicantResponseDTO updated = applicantService.updateApplicantByAdmin(id, dto, adminUsername);
        return ResponseEntity.ok(updated);
    }
}

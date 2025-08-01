package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.service.ApplicantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applicants")
public class ApplicantController {

    @Autowired
    private ApplicantService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<ApplicantResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAllApplicants());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<ApplicantResponseDTO>> search(
            @RequestParam(required = false) Long ficha,
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String career,
            @RequestParam(required = false, name = "fullName") String fullName) {
        return ResponseEntity.ok(
                service.searchApplicants(ficha, curp, career, fullName));
    }
}

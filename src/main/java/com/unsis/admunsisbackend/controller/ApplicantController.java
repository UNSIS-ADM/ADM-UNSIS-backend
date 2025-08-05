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
    private ApplicantService applicantService;

    @Autowired
    private ApplicantService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public ResponseEntity<List<ApplicantResponseDTO>> getAllApplicants() {
        List<ApplicantResponseDTO> list = applicantService.getAllApplicants();
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{curp}/career")
    @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
    public ResponseEntity<?> changeCareerByCurp(
            @PathVariable("curp") String curp,
            @RequestParam("career") String newCareer) {
        service.changeCareerByCurp(curp, newCareer);
        return ResponseEntity.noContent().build();
    }
    


}

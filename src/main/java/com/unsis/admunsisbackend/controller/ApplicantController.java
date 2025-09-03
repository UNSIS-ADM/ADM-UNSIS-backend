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
    public ResponseEntity<List<ApplicantResponseDTO>> getAll(
        @RequestParam(value = "year", required = false) Integer year) {
        return ResponseEntity.ok(service.getAllApplicants(year));
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

    @PutMapping("/{curp}/career")
    @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
    public ResponseEntity<?> changeCareerByCurp(
            @PathVariable("curp") String curp, 
            @RequestParam("career") String newCareer) {
        service.changeCareerByCurp(curp, newCareer);
        return ResponseEntity.noContent().build();
    }
    


}
